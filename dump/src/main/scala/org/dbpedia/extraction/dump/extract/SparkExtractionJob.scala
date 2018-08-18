package org.dbpedia.extraction.dump.extract

import java.io.File
import java.net.URL

import org.apache.hadoop.io.compress.BZip2Codec
import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
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
                         context: SparkExtractionContext,
                         namespaces: Set[Namespace],
                         destination: Destination,
                         lang: Language,
                         retryFailedPages: Boolean,
                         extractionRecorder: ExtractionRecorder[WikiPage]) {

  def run(spark: SparkSession, config: Config): Unit = {
    val sparkContext = spark.sparkContext

    try {
      //broadcasts
      val broadcastNamespaces = sparkContext.broadcast(namespaces)
      val broadcastOntology = sparkContext.broadcast(context.ontology)
      val broadcastLanguage = sparkContext.broadcast(context.language)
      val broadcastRedirects = sparkContext.broadcast(context.redirects)
      val broadcastDisambiguations = sparkContext.broadcast(context.disambiguations)
      val broadcastFormats = sparkContext.broadcast(config.formats)
      val broadcastExtractors = sparkContext.broadcast(extractors)


      //finding the source files
      val fileFinder = new Finder[File](config.dumpDir, context.language, config.wikiName)
      val latestDate = getLatestDate(fileFinder, config)
      val dumpSources = config.source.flatMap(x => getInputFiles(x, fileFinder, latestDate))

      //number of worker-cores
      val numberOfCores = sparkContext.defaultParallelism

      //initialize extraction-recorder
      val extractor = CompositeParseExtractor.load(extractors, context)
      extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
      extractor.initializeExtractor()

      //extraction
      dumpSources.foreach(file => {
        extractionRecorder.printLabeledLine(s"Starting Extraction on ${file.getName}.", RecordSeverity.Info)
        /* ------- READING    -------
          * Read XML-Dump from baseDir, delimit the text by the <page> tag
          * and create an RDD from it
          */
        val inputConfiguration = new org.apache.hadoop.conf.Configuration
        inputConfiguration.set("textinputformat.record.delimiter", "<page>")
        val wikiPageXmlRDD : RDD[String] =
          sparkContext.newAPIHadoopFile(file.getAbsolutePath, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], inputConfiguration)
            .map("<page>" + _._2.toString.trim).repartition(numberOfCores)

        /* ------- PARSING    -------
         * Parse the String to WikiPage Objects
         */
        val wikiPageParsedRDD : RDD[WikiPage] = wikiPageXmlRDD.flatMap(SerializableUtils.parseXMLToWikiPage(_, broadcastLanguage.value))

        /* ------- EXTRACTION -------
         * Build extractor-context from broadcast values once per partition.
         * Extract quads.
         * Flatten the Quad Collections.
         */
        val extractedDataRDD : RDD[Quad] = wikiPageParsedRDD.mapPartitions(pages => {
          // TODO: ImageExtractor
          def worker_context =
            new SparkExtractionContext {
              def ontology: Ontology = broadcastOntology.value
              def language: Language = broadcastLanguage.value
              def redirects: Redirects = broadcastRedirects.value
              def disambiguations: Disambiguations = broadcastDisambiguations.value
            }
          val localExtractor = CompositeParseExtractor.load(broadcastExtractors.value, worker_context)
          pages.map(page =>
            SerializableUtils.extractQuadsFromPage(broadcastNamespaces.value, localExtractor, page)
          )
        }).flatMap(identity)

        /* ------- WRITING    -------
         * Prepare the lines that will be written.
         * ⇒ generate the key that determines the destination file, and let the formatter render the quad as string.
         * Lastly let each worker write its own file and use a bash script to concat these files together afterwards.
         */
        extractedDataRDD.mapPartitionsWithIndex(
          (partition, quads) => quads.flatMap(quad =>
            broadcastFormats.value.flatMap(formats =>
              Try{(Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)}.toOption
            )
          )
        ).saveAsHadoopFile(s"${file.getParent}/_temporary/", classOf[Key], classOf[String], classOf[CustomPartitionedOutputFormat], classOf[BZip2Codec])
        concatOutputFiles(new File(s"${file.getParent}/_temporary/"), s"${lang.wikiCode}${config.wikiName}-$latestDate-", broadcastFormats.value.keys)
        extractionRecorder.printLabeledLine(s"Finished Extraction on ${file.getName}.", RecordSeverity.Info)
      })

      if (retryFailedPages) {
        //output dir
        val dir = dumpSources.last.getParent
        //pages to retry
        val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
        extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
        extractionRecorder.resetFailedPages(lang)

        //already in the memory → use makeRDD
        val failedPagesRDD = sparkContext.makeRDD(failedPages)

        /* ------- EXTRACTION -------
          * Build extractor-context from broadcast values once per partition.
          * Extract quads.
          * Flatten the Quad Collections.
          */
        val failResults = failedPagesRDD.mapPartitions(pages => {

          def worker_context =
            // MappingsExtractor is not used
            new SparkExtractionContext {
              def ontology: Ontology = broadcastOntology.value
              def language: Language = broadcastLanguage.value
              def redirects: Redirects = broadcastRedirects.value
              def disambiguations: Disambiguations = broadcastDisambiguations.value
            }
          val localExtractor = CompositeParseExtractor.load(broadcastExtractors.value, worker_context)
          pages.map(page =>
            SerializableUtils.extractQuadsFromPage(broadcastNamespaces.value, localExtractor, page)
          )
        }).flatMap(identity)

        /* ------- WRITING    -------
          * Prepare the lines that will be written.
          * => generate the key that determines the destination file, and let the formatter render the quad as string.
          * Lastly let each worker write its own file and use a bash script to concat these files together afterwards.
          */
        failResults.mapPartitionsWithIndex(
          (partition, quads) => quads.flatMap(quad =>
            broadcastFormats.value.toSeq.flatMap(formats =>
              Try{(Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)}.toOption
            )
          )
        ).saveAsHadoopFile(s"$dir/_temporary/", classOf[Key], classOf[String], classOf[CustomPartitionedOutputFormat], classOf[BZip2Codec])
        concatOutputFiles(new File(s"$dir/_temporary/"), s"${lang.wikiCode}${config.wikiName}-$latestDate-", broadcastFormats.value.keys)
      }

      //FIXME Extraction Monitor not working

      // finalize extraction-recorder
      extractionRecorder.printLabeledLine("finished complete extraction after {page} pages with {mspp} per page", RecordSeverity.Info, lang)
      extractor.finalizeExtractor()
      extractionRecorder.finalize()
    }
    catch {
      case ex: Throwable =>
//        if (extractionRecorder.monitor != null) extractionRecorder.monitor.reportCrash(extractionRecorder, ex)
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

  private def getLatestDate(finder: Finder[_], config: Config): String = {
    val isSourceRegex = config.source.startsWith("@")
    val source = if (isSourceRegex) config.source.head.substring(1) else config.source.head
    val fileName = if (config.requireComplete) Config.Complete else source
    finder.dates(fileName, isSuffixRegex = isSourceRegex).last
  }

  private def getInputFiles(source: String, finder: Finder[File], date: String): List[File] = {
    val files = if (source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
      finder.matchFiles(date, source.substring(1))
    } else List(finder.file(date, source)).collect{case Some(x) => x}
    files
  }

  private def concatOutputFiles(dir : File, prefix: String, formats: Iterable[String]): Unit = {
    dir.listFiles().foreach(datasetDir => {
      if(datasetDir.isDirectory) {
        val ds = datasetDir.getName.replace("_","-")
        formats.foreach(format =>
          Seq("bash", "../scripts/src/main/bash/concatFiles.sh",
            datasetDir.getAbsolutePath, s"${dir.getParent}/$prefix$ds.$format", format).!)
      }
    })
    deleteFilesRecursively(dir)
  }

  private def deleteFilesRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteFilesRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }
}

/**
  * Custom output format for the saveAsHadoopFile method.
  * Each partition writes its own files, datasets get their own folder
  */
class CustomPartitionedOutputFormat extends MultipleTextOutputFormat[Any, Any] {
  override def generateActualKey(key: Any, value: Any): Any =
    NullWritable.get()

  override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String = {
    val k = key.asInstanceOf[Key]
    val format = k.format.split(".bz2")(0)
    s"${k.dataset}/${k.partition}.$format"
  }
}

case class Key(dataset: String, format: String, partition: Int)


