package org.dbpedia.extraction.dump.extract

import java.io.File

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.BZip2Codec
import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.LogManager
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}

import scala.collection.mutable
import scala.sys.process._
import scala.util.Try
import scala.xml.XML.loadString

/**
  * Current time-comparison:
  * Test Specs:               10000 pages, dewiki, 8 cores
  * Live:                     ~1:22 min
  *                           ---------
  * spark-core:               ~1:51 min
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

  val logger = LogManager.getRootLogger()

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
        extractionRecorder.printLabeledLine(s"Starting Extraction on ${file.getName}.", RecordSeverity.Info)

        val source = file.getAbsolutePath
        val bc_dir = sparkContext.broadcast(file.getParent)

        /** ------- READING    -------
          * Read XML-Dump from baseDir, delimit the text by the <page> tag
          * and create an RDD from it
          */
        val conf = new Configuration
        conf.set("textinputformat.record.delimiter", "<page>")
        val xmlRDD =
          sparkContext.newAPIHadoopFile(source, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], conf)
          .map(x => s"<page>${x._2.toString}") // repair the xml
          .repartition(numberOfCores)

        /** ------- PARSING    -------
          * Parse the String to WikiPage Objects
          */
        val wikiPageRDD = xmlRDD.flatMap(SerializableUtils.xmlToWikiPage)

        /** ------- EXTRACTION -------
          * Build extractor-context from broadcasted values once per partition.
          * Extract quads
          */
        val results = wikiPageRDD.mapPartitions(pages => {
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
        })

        /** ------- WRITING    -------
          * Flatten the Quad Collections and then prepare the lines that will be written.
          * => generate the key that determines the destination file, and let the formatter render the quad as string.
          * Lastly let each worker write its own file and use a bash script to concat these files together afterwards.
          */
        results.flatMap(identity)
          .mapPartitionsWithIndex(
            (partition, quads) => quads.flatMap(quad =>
              bc_formats.value.map(formats =>
                (Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)
              )
            )
          )
          .saveAsHadoopFile(s"${bc_dir.value}/_temporary/", classOf[Key], classOf[String], classOf[OutputFormat], classOf[BZip2Codec])
        concatFiles(new File(s"${bc_dir.value}/_temporary/"), s"${lang.wikiCode}${config.wikiName}-$date-", bc_formats.value.keys)
        extractionRecorder.printLabeledLine(s"Finished Extraction on ${file.getPath}.", RecordSeverity.Info)
      })

      //TODO retry failed pages
      if (retryFailedPages) {
//        val dir = sources.last.getParent
//
//        val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
//        extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
//        extractionRecorder.resetFailedPages(lang)
//        val failedPagesRDD = sparkContext.parallelize(failedPages)
//
//        // ------- EXTRACTION -------
//        val failResults = failedPagesRDD.mapPartitions(pages => {
//          case class _WikiPage(title: WikiTitle, redirect: WikiTitle, id: Long, revision: Long, timestamp: Long, contributorID: Long, contributorName: String, source: String, format: String)
//
//          //create extractor from broadcasted values
//          def worker_context = new DumpExtractionContext {
//            def ontology: Ontology = bc_ontology.value
//            def language: Language = bc_language.value
//            def redirects: Redirects = bc_redirects.value
//            def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
//          }
//          val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)
//          pages.map(page => {
//            SerializableMethods.processPage(bc_namespaces.value, localExtractor, page)
//          })
//        })
//
//        // ------- WRITE      -------
//        val failQuads = failResults.flatMap(quads => quads.getOrElse(Seq[Quad]()))
//          .map(quad => (quad.dataset, ""))
//          .toDF("dataset", "quads")
//        try {
//          failQuads.write.partitionBy("dataset").option("compression", "bzip2").text(dir + "/failed_out/")
//        } catch {
//          case ex: Throwable => ex.printStackTrace()
//        }
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
          Seq("bash", "./scripts/src/main/bash/concatFiles.sh",
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
    s"${k.dataset}/${k.partition}.${format}"
  }
}

case class Key(dataset: String, format: String, partition: Int)

object SerializableUtils extends Serializable {
  @transient lazy val logger = LogManager.getRootLogger

  /**
    * Extracts Quads from a WikiPage
    * checks for correct namespace
    * deduplication
    * @param namespaces Set of correct namespaces
    * @param extractor Wikipage Extractor
    * @param page WikiPage
    * @return Deduplicated Colection of Quads
    */
  def processPage(namespaces: Set[Namespace], extractor: Extractor[WikiPage], page: WikiPage): Seq[Quad] = {
    //LinkedHashSet for deduplication
    val uniqueQuads = new mutable.LinkedHashSet[Quad]()
    try {
      if (namespaces.exists(_.equals(page.title.namespace))) {
        //Extract Quads
        uniqueQuads ++= extractor.extract(page, page.uri)
      }
      else {
        logger.warn("wrong namespace for page " + page.title.decoded)
        //FIXME: happens very often on big files
      }
    }
    catch {
      case ex: Exception =>
        logger.error("error while processing page " + page.title.decoded, ex)
        page.addExtractionRecord(null, ex)
        page.getExtractionRecords()
    }
    uniqueQuads.toSeq
  }

  /**
    * Parses a xml string to a wikipage.
    * TODO: clean up
    * @param xmlString xml
    * @return WikiPage Option
    */
  def xmlToWikiPage(xmlString: String): Option[WikiPage] = {
    if(!xmlString.trim.split("\n").last.contains("</page>")) None
    else try {
      val page = loadString(xmlString)
      val language = Language("de")
      val rev = page \ "revision"
      val title = WikiTitle.parseCleanTitle((page \ "title").text, language, Try {
        new java.lang.Long(java.lang.Long.parseLong((page \ "id").text))
      }.toOption)
      val nsElem = page \ "ns"
      if (nsElem.nonEmpty) {
        try {
          val nsCode = nsElem.text.toInt
          require(title.namespace.code == nsCode, "XML Namespace (" + nsCode + ") does not match the computed namespace (" + title.namespace + ") in page: " + title.decodedWithNamespace)
        }
        catch {
          case e: NumberFormatException => throw new IllegalArgumentException("Cannot parse content of element [ns] as int", e)
        }
      }
      //Skip bad titles
      if (title != null) {
        val _redirect = (page \ "redirect" \ "@title").text match {
          case "" => null
          case t => WikiTitle.parse(t, language)
        }
        val _contributorID = (rev \ "contributor" \ "id").text match {
          case null => "0"
          case id => id
        }
        Some(new WikiPage(title = title,
          redirect = _redirect,
          id = (page \ "id").text,
          revision = (rev \ "id").text,
          timestamp = (rev \ "timestamp").text,
          contributorID = _contributorID,
          contributorName = if (_contributorID == "0") (rev \ "contributor" \ "ip").text
          else (rev \ "contributor" \ "username").text,
          source = (rev \ "text").text,
          format = (rev \ "format").text))
      }
      else None
    } catch {
      case ex: Throwable =>
        logger.warn("error parsing xml:" + ex.getMessage)
        None
    }
  }

}
