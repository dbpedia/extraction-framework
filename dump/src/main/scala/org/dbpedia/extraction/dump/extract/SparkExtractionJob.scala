package org.dbpedia.extraction.dump.extract

import java.io.File

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.compress.BZip2Codec
import org.apache.hadoop.io.{LongWritable, NullWritable, Text}
import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat
import org.apache.log4j.{LogManager, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage}

import scala.collection.mutable
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

  val logger: Logger = LogManager.getRootLogger
  val broadcastValues: mutable.HashMap[String, Broadcast[Any]] = mutable.HashMap[String, Broadcast[Any]]()
  val temporaryDirectoryName = "/_temporary/"
  //FIXME Extraction Monitor not working
  def run(spark: SparkSession, config: Config): Unit = {
    val sparkContext = spark.sparkContext
    try {
      val fileFinder = new Finder[File](config.dumpDir, context.language, config.wikiName)
      val latestDate = getLatestDate(fileFinder, config)
      val dumpSources = config.source.flatMap(x => getInputFiles(x, fileFinder, latestDate))
      val dir = dumpSources.head.getParent
      val concatDir = new File(dir + temporaryDirectoryName)
      val prefix = s"${lang.wikiCode}${config.wikiName}-$latestDate-"
      val formatKeys = config.formats.keys

      broadcastValues.put("formats", sparkContext.broadcast(config.formats))
      broadcastValues.put("namespaces", sparkContext.broadcast(namespaces))
      broadcastValues.put("extractors", sparkContext.broadcast(extractors))
      broadcastValues.put("ontology", sparkContext.broadcast(context.ontology))
      broadcastValues.put("language", sparkContext.broadcast(context.language))
      broadcastValues.put("redirects", sparkContext.broadcast(context.redirects))
      broadcastValues.put("dir", sparkContext.broadcast(dumpSources.head.getParent))

      val extractor = CompositeParseExtractor.load(extractors, context)
      extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
      extractor.initializeExtractor()

      dumpSources.foreach(file => {
        extractionRecorder.printLabeledLine(s"Starting Extraction on ${file.getName}", RecordSeverity.Info)
        val wikiPageXmlRDD : RDD[String] = readDump(file.getAbsolutePath, sparkContext)
        val wikiPageParsedRDD : RDD[WikiPage] = parseXML(wikiPageXmlRDD, broadcastValues)
        val extractedDataRDD : RDD[Quad] = extractQuads(wikiPageParsedRDD, broadcastValues)
        writeData(extractedDataRDD, broadcastValues)
        concatOutputFiles(concatDir, prefix, formatKeys)
        extractionRecorder.printLabeledLine(s"Finished Extraction on ${file.getName}", RecordSeverity.Info)
      })

      if (retryFailedPages) {
        val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
        extractionRecorder.printLabeledLine(s"retrying ${failedPages.length} failed pages", RecordSeverity.Warning, lang)
        extractionRecorder.resetFailedPages(lang)
        //already in the memory -> use makeRDD
        val failedPagesRDD = sparkContext.makeRDD(failedPages)
        val failResults = extractQuads(failedPagesRDD, broadcastValues)
        writeData(failResults, broadcastValues)
        concatOutputFiles(concatDir, prefix, formatKeys)
        extractionRecorder.printLabeledLine(s"Finished Extraction on the failed pages", RecordSeverity.Info)
      }

      extractionRecorder.printLabeledLine("finished complete extraction after {page} pages with {mspp} per page", RecordSeverity.Info, lang)
      extractor.finalizeExtractor()
      extractionRecorder.finalize()
    }
    catch {
      case ex: Throwable =>
        val e = ex
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

  private def readDump(path: String, sparkContext: SparkContext): RDD[String] = {
    val numberOfCores = sparkContext.defaultParallelism
    val inputConfiguration = new Configuration
    inputConfiguration.set("textinputformat.record.delimiter", "<page>")
    sparkContext.newAPIHadoopFile(path, classOf[TextInputFormat], classOf[LongWritable], classOf[Text], inputConfiguration)
        .map("<page>" + _._2.toString.trim).repartition(numberOfCores)
  }

  private def parseXML(rdd: RDD[String], broadcastMap: mutable.HashMap[String, Broadcast[Any]]): RDD[WikiPage] = {
    val broadcastLanguage = broadcastMap("language")
    rdd.flatMap(SerializableUtils.parseXMLToWikiPage(_, broadcastLanguage.value.asInstanceOf[Language]))
  }

  private def extractQuads(rdd: RDD[WikiPage], broadcastMap: mutable.HashMap[String, Broadcast[Any]]): RDD[Quad] = {
    val broadcastOntology = broadcastMap("ontology")
    val broadcastLanguage = broadcastMap("language")
    val broadcastRedirects = broadcastMap("redirects")
    val broadcastExtractors = broadcastMap("extractors")
    val broadcastNamespace = broadcastMap("namespaces")

    rdd.mapPartitions(pages => {
      def worker_context = new SparkExtractionContext {
        def ontology: Ontology = broadcastOntology.value.asInstanceOf[Ontology]
        def language: Language = broadcastLanguage.value.asInstanceOf[Language]
        def redirects: Redirects = broadcastRedirects.value.asInstanceOf[Redirects]
      }
      val localExtractor = CompositeParseExtractor.load(broadcastExtractors.value.asInstanceOf[Seq[Class[Extractor[_]]]], worker_context)
      pages.map(page =>
        SerializableUtils.extractQuadsFromPage(broadcastNamespace.value.asInstanceOf[Set[Namespace]], localExtractor, page)
      )
    }).flatMap(identity)
  }

  private def writeData(rdd: RDD[Quad],  broadcastMap: mutable.HashMap[String, Broadcast[Any]]): Unit = {
    val broadcastFormats = broadcastMap("formats")
    val broadcastDir = broadcastMap("dir")
    rdd.mapPartitionsWithIndex(
      (partition, quads) => quads.flatMap(quad =>
        broadcastFormats.value.asInstanceOf[Map[String, Formatter]].flatMap(formats =>
          Try{(Key(quad.dataset, formats._1, partition), formats._2.render(quad).trim)}.toOption
        )
      )
    ).saveAsHadoopFile(s"${broadcastDir.value.asInstanceOf[String]} /_temporary/", classOf[Key], classOf[String], classOf[CustomPartitionedOutputFormat], classOf[BZip2Codec])
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
    logger.info(s"Source is $source - ${files.size} file(s) matched")
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


