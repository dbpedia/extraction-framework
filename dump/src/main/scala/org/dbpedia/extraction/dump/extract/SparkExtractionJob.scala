package org.dbpedia.extraction.dump.extract

import java.io.File

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

import scala.sys.process._
import scala.util.Try
import scala.xml.XML.loadString

/**
  * Current time-comparison:
  * Test Size:                10000 pages
  * Live:                     ~1:22 min
  *                           ---------
  * spark-core:               ~1:51 min
  * spark-sql with spark-xml: ~1:01 min
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
    import spark.implicits._

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

      val finder = new Finder[File](config.dumpDir, context.language, config.wikiName)
      val date = latestDate(finder, config)
      val sources = config.source.flatMap(x => files(x, finder, date))

      //initialize extraction
      val extractor = CompositeParseExtractor.load(extractors, context)
      extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
      extractor.initializeExtractor()

      sources.foreach(file => {
        val source = file.getAbsolutePath
        val bc_dir = sparkContext.broadcast(file.getParent)
        extractionRecorder.printLabeledLine(s"Starting Extraction on ${file.getPath}.", RecordSeverity.Info)

        // ------- READING    -------
        val df = spark.read
          .format("com.databricks.spark.xml")
          .option("rowTag", "page")
          .load(source)
          .repartition(64)

        // ------- PARSING    -------
        val wikiPages = df.rdd.mapPartitions(xmlPages => {
          @transient lazy val logger = LogManager.getRootLogger
          // Helper-Objects
          case class Page(id: Option[java.lang.Long], namespace: Long, revision: Revision, title: String)
          case class Revision(contributor: Contributor, format: String, id: Long, text: Text, timestamp: String)
          case class Text(value: String, space: String)
          case class Contributor(id: Option[Long], username: Option[String])
          case class _WikiPage(title: WikiTitle, redirect: WikiTitle, id: Long, revision: Long, timestamp: Long, contributorID: Long, contributorName: String, source: String, format: String)
          xmlPages.flatMap(row => {
            try {
              val rev = row.getStruct(2)
              val contributor = rev.getStruct(0)
              val contributorParsed = Contributor(Try {
                contributor.getLong(0)
              }.toOption, Try {
                contributor.getString(1)
              }.toOption)
              val text = rev.getStruct(3)
              val textParsed = Text(text.getString(0), text.getString(1))
              val revParsed = Revision(contributorParsed, rev.getString(1), rev.getLong(2), textParsed, rev.getString(4))
              val page = Page(Try {
                row.getLong(0).asInstanceOf[java.lang.Long]
              }.toOption, row.getLong(1), revParsed, row.getString(3))
              val title = WikiTitle.parseCleanTitle(page.title, bc_language.value, page.id)

              Some(new WikiPage(title,
                null,
                page.id.getOrElse("").toString,
                page.revision.id.toString,
                page.revision.timestamp,
                page.revision.contributor.id.getOrElse("0").toString,
                page.revision.contributor.username.getOrElse(""),
                page.revision.text.value,
                page.revision.format))
            }
            catch {
              case ex: Throwable => logger.error("error parsing page", ex)
                None
            }
          })
        })

        // ------- EXTRACTION -------
        val results = wikiPages.mapPartitions(pages => {
          case class _WikiPage(title: WikiTitle, redirect: WikiTitle, id: Long, revision: Long, timestamp: Long, contributorID: Long, contributorName: String, source: String, format: String)

          //create extractor from broadcasted values
          def worker_context = new DumpExtractionContext {
            def ontology: Ontology = bc_ontology.value
            def language: Language = bc_language.value
            def redirects: Redirects = bc_redirects.value
            def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
          }
          val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)
          pages.map(page => {
            SerializableUtils.processPage(bc_namespaces.value, localExtractor, page).getOrElse(Seq[Quad]())
          })
        })

        val data = results.flatMap(identity).flatMap(quad => {
          bc_formats.value.map(format => (quad.dataset, format._1, format._2.render(quad)))
        }).toDF("dataset", "format", "string")

        // ------- WRITING    -------
        try {
          data.write.partitionBy("dataset", "format").option("compression", "bzip2").text(bc_dir.value + "/tmp_out/")
          concatFiles(new File(sources.last.getParent + "/tmp_out/"), s"${lang.wikiCode}${config.wikiName}-$date-")
        } catch {
          case ex: Throwable => ex.printStackTrace()
        }

        extractionRecorder.printLabeledLine(s"Finished Extraction on ${file.getPath}.", RecordSeverity.Info)
      })

//      // Retry failed pages
//      if (retryFailedPages) {
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
//      }
      // Finish up Extraction
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

  private def concatFiles(dir : File, prefix: String): Unit = {
    dir.listFiles().foreach(datasetDir => {
      if(datasetDir.isDirectory) {
        val ds = datasetDir.getName.split("=")(1).replace("_","-")
        datasetDir.listFiles.foreach(formatDir => {
          val format = formatDir.getName.split("=")(1)
          Seq("bash", "./scripts/src/main/bash/concatFiles.sh",
            formatDir.getAbsolutePath, s"${dir.getParent}/$prefix$ds.$format").!
        })
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

object SerializableUtils extends Serializable {
  @transient lazy val logger = LogManager.getRootLogger

  def processPage(namespaces: Set[Namespace], extractor: Extractor[WikiPage], page: WikiPage): Option[Seq[Quad]] = {
    try {
      //      // Generate Logs
      //      val records = page.getExtractionRecords() match {
      //        case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
      //        case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
      //      }
      // Extract Quads
      if (namespaces.exists(_.equals(page.title.namespace))) {
        Some(extractor.extract(page, page.uri))
      }
      else {
        logger.warn("wrong namespace for page " + page.title.decoded)
        None
      }
    }
    catch {
      case ex: Exception =>
        logger.error("error while processing page " + page.title.decoded, ex)
        page.addExtractionRecord(null, ex)
        page.getExtractionRecords()
        None
    }
  }

  def xmlToWikiPage(xmlString: String): Option[WikiPage] = {
    val xml = loadString(xmlString)
    val language = Language("de")
    val page = xml \ "page"
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
  }

}
