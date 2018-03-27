package org.dbpedia.extraction.dump.extract

import java.io.File

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.BZip2Codec
import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}

import scala.sys.process._
import scala.util.Try

/**
  * Current time-comparison:
  * Test Specs:               10000 pages, dewiki, 8 cores
  * Live:                     ~1:22 min
  *                           ---------
  * spark-sql with spark-xml: ~1:08 min
  * custom Hadoop In-/Output: ~0:43 min
  *
  * @param extractors The Extractors
  * @param context    The Extraction Context
  * @param namespaces Only extract pages in these namespaces
  * @param lang       the language of this extraction.
  */
class SparkExtractionJob(extractors: Seq[Class[_ <: Extractor[_]]],
                         context: DumpExtractionContext,
                         namespaces: Set[Namespace],
                         destination: Destination,
                         lang: Language,
                         retryFailedPages: Boolean,
                         extractionRecorder: ExtractionRecorder[WikiPage]) {

  val logger: Logger = LogManager.getRootLogger

  def run(spark: SparkSession, config: Config): Unit = {
    val sparkContext = spark.sparkContext

    try {
      //broadcasts
      val bc_namespaces = sparkContext.broadcast(namespaces)
      val bc_ontology = sparkContext.broadcast(context.ontology)
      val bc_language = sparkContext.broadcast(context.language)
      val bc_redirects = sparkContext.broadcast(context.redirects)
      val bc_mappingPageSource = sparkContext.broadcast(context.mappingPageSource)

      val bc_formats = sparkContext.broadcast(config.formats)
      val bc_extractors = sparkContext.broadcast(extractors)

      //input files
      val finder = new Finder[File](config.dumpDir, context.language, config.wikiName)
      val date = latestDate(finder, config)
      val sources = config.source.flatMap(x => files(x, finder, date))

      //number of worker-cores
      val numberOfCores = sparkContext.defaultParallelism

      //initialize extraction-recorder
      val extractor = CompositeParseExtractor.load(extractors, context)
      extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
      extractor.initializeExtractor()

      //extraction
      sources.foreach(file => {
        val source = file.getAbsolutePath
        val bc_dir = sparkContext.broadcast(file.getParent)
        extractionRecorder.printLabeledLine(s"Starting Extraction on ${file.getName}.", RecordSeverity.Info)

        /* ------- READING    -------
          * Read XML-Dump from baseDir, delimit the text by the <page> tag
          * and create an RDD from it
          */
        val conf = new Configuration
        conf.set("textinputformat.record.delimiter", "<page>")
        val xmlRDD : RDD[String] =
          sparkContext.newAPIHadoopFile(source, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], conf)
          .map("<page>" + _._2.toString.trim).repartition(numberOfCores)

        /* ------- PARSING    -------
         * Parse the String to WikiPage Objects
         */
        val wikiPageRDD : RDD[WikiPage] = xmlRDD.flatMap(SerializableUtils.xmlToWikiPage(_, bc_language.value))

        /* ------- EXTRACTION -------
         * Build extractor-context from broadcasted values once per partition.
         * Extract quads.
         * Flatten the Quad Collections.
         */
        val results : RDD[Quad] = wikiPageRDD.mapPartitions(pages => {
          def worker_context = new DumpExtractionContext {
            def ontology: Ontology = bc_ontology.value
            def language: Language = bc_language.value
            def redirects: Redirects = bc_redirects.value
            def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
          }
          val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)
          pages.map(page =>
            SerializableUtils.processPage(bc_namespaces.value, localExtractor, page)
          )
        }).flatMap(identity)

        /* ------- WRITING    -------
         * Prepare the lines that will be written.
         * => generate the key that determines the destination file, and let the formatter render the quad as string.
         * Lastly let each worker write its own file and use a bash script to concat these files together afterwards.
         */
        results.mapPartitionsWithIndex(
            (partition, quads) => quads.flatMap(quad =>
              bc_formats.value.flatMap(formats =>
                Try{(Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)}.toOption
              )
            ).toSeq.distinct.toIterator
          )
          .saveAsHadoopFile(s"${bc_dir.value}/_temporary/", classOf[Key], classOf[String], classOf[OutputFormat], classOf[BZip2Codec])
        concatFiles(new File(s"${bc_dir.value}/_temporary/"), s"${lang.wikiCode}${config.wikiName}-$date-", bc_formats.value.keys)
        extractionRecorder.printLabeledLine(s"Finished Extraction on ${file.getName}.", RecordSeverity.Info)
      })

      if (retryFailedPages) {
        //output dir
        val dir = sources.last.getParent
        //pages to retry
        val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
        extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
        extractionRecorder.resetFailedPages(lang)

        //already in the memory -> use makeRDD
        val failedPagesRDD = sparkContext.makeRDD(failedPages)

        /* ------- EXTRACTION -------
          * Build extractor-context from broadcasted values once per partition.
          * Extract quads.
          * Flatten the Quad Collections.
          */
        val failResults = failedPagesRDD.mapPartitions(pages => {
          case class _WikiPage(title: WikiTitle, redirect: WikiTitle, id: Long, revision: Long, timestamp: Long, contributorID: Long, contributorName: String, source: String, format: String)

          def worker_context = new DumpExtractionContext {
            def ontology: Ontology = bc_ontology.value
            def language: Language = bc_language.value
            def redirects: Redirects = bc_redirects.value
            def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
          }
          val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)
          pages.map(page =>
            SerializableUtils.processPage(bc_namespaces.value, localExtractor, page)
          )
        }).flatMap(identity)

        /* ------- WRITING    -------
          * Prepare the lines that will be written.
          * => generate the key that determines the destination file, and let the formatter render the quad as string.
          * Lastly let each worker write its own file and use a bash script to concat these files together afterwards.
          */
        failResults.mapPartitionsWithIndex(
            (partition, quads) => quads.flatMap(quad =>
              bc_formats.value.toSeq.flatMap(formats =>
                Try{(Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)}.toOption
              )
            )
          ).distinct().saveAsHadoopFile(s"$dir/_temporary/", classOf[Key], classOf[String], classOf[OutputFormat], classOf[BZip2Codec])
        concatFiles(new File(s"$dir/_temporary/"), s"${lang.wikiCode}${config.wikiName}-$date-", bc_formats.value.keys)
      }

      //FIXME Extraction Monitor not working

      // finalize extraction-recorder
      extractionRecorder.printLabeledLine("finished complete extraction after {page} pages with {mspp} per page", RecordSeverity.Info, lang)
      extractor.finalizeExtractor()
      extractionRecorder.finalize()
    }
    catch {
      case ex: Throwable =>
        if (extractionRecorder.monitor != null) extractionRecorder.monitor.reportCrash(extractionRecorder, ex)
        ex.printStackTrace()
        extractionRecorder.finalize()
    }
  }

  def createRecords(page: WikiPage): Seq[RecordEntry[WikiPage]] = {
    page.getExtractionRecords() match {
      case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
      case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
    }
  }

  private def latestDate(finder: Finder[_], config: Config): String = {
    val isSourceRegex = config.source.startsWith("@")
    val source = if (isSourceRegex) config.source.head.substring(1) else config.source.head
    val fileName = if (config.requireComplete) Config.Complete else source
    finder.dates(fileName, isSuffixRegex = isSourceRegex).last
  }

  private def files(source: String, finder: Finder[File], date: String): List[File] = {
    val files = if (source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
      finder.matchFiles(date, source.substring(1))
    } else List(finder.file(date, source)).collect{case Some(x) => x}
    logger.info(s"Source is $source - ${files.size} file(s) matched")
    files
  }

  private def concatFiles(dir : File, prefix: String, formats: Iterable[String]): Unit = {
    dir.listFiles().foreach(datasetDir => {
      if(datasetDir.isDirectory) {
        val ds = datasetDir.getName.replace("_","-")
        formats.foreach(format =>
          Seq("bash", "../scripts/src/main/bash/concatFiles.sh",
          datasetDir.getAbsolutePath, s"${dir.getParent}/$prefix$ds.$format", format).!)
      }
    })
    deleteRecursively(dir)
  }

  private def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }
}

/**
  * Custom output format for the saveAsHadoopFile method.
  * Each partition writes its own files, datasets get their own folder
  */
class OutputFormat extends MultipleTextOutputFormat[Any, Any] {
  override def generateActualKey(key: Any, value: Any): Any =
    NullWritable.get()

  override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String = {
    val k = key.asInstanceOf[Key]
    val format = k.format.split(".bz2")(0)
    s"${k.dataset}/${k.partition}.$format"
  }
}

case class Key(dataset: String, format: String, partition: Int)


