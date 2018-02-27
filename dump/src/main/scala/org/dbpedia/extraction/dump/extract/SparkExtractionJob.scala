package org.dbpedia.extraction.dump.extract

import org.apache.log4j.LogManager
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}

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
      val bc_base_dir = sparkContext.broadcast(config.dumpDir)
      val bc_extractors = sparkContext.broadcast(extractors)

      //initialize extraction
      val extractor = CompositeParseExtractor.load(extractors, context)
      extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
      extractor.initializeExtractor()

      // ------- READING    -------
      val df = spark.read
        .format("com.databricks.spark.xml")
        .option("rowTag", "page")
        .load("/data/2016-10/dewiki/20161020/dewiki-20161020-pages-articles.xml.bz2")

      // TODO: Might be even faster with Kafka and/or spark.readStream

      // ------- PARSING    -------
      val wikiPages = df.rdd.repartition(64).mapPartitions(xmlPages => {
        @transient lazy val logger = LogManager.getRootLogger

        // Helper-Objects
        case class Page(id: Option[java.lang.Long], namespace: Long, revision: Revision, title: String)
        case class Revision(contributor: Contributor, format: String, id: Long, text: Text, timestamp: String)
        case class Text(value: String, space: String)
        case class Contributor(id: Option[Long], username: Option[String])

        xmlPages.flatMap(row => {
          try {
            val rev = row.getStruct(2)
            val contributor = rev.getStruct(0)
            val contributorParsed = Contributor(Try{contributor.getLong(0)}.toOption, Try{contributor.getString(1)}.toOption)
            val text = rev.getStruct(3)
            val textParsed = Text(text.getString(0), text.getString(1))
            val revParsed = Revision(contributorParsed, rev.getString(1), rev.getLong(2), textParsed, rev.getString(4))
            val page = Page(Try{row.getLong(0).asInstanceOf[java.lang.Long]}.toOption, row.getLong(1), revParsed, row.getString(3))
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
            case ex : Throwable => logger.error("error parsing page", ex)
              None
          }
        })
      })

      // ------- EXTRACTION -------
      val results = wikiPages.mapPartitions(pages => {
        //create extractor from broadcasted values
        def worker_context = new DumpExtractionContext {
          def ontology: Ontology = bc_ontology.value

          def language: Language = bc_language.value

          def redirects: Redirects = bc_redirects.value

          def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
        }

        val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)

        pages.map(SerializableMethods.processPage(bc_namespaces.value, localExtractor, _))
      })

      // ------- WRITE      -------
      val quads = results.flatMap(quads => quads.getOrElse(Seq[Quad]()))
        .map(quad => (quad.dataset, SerializableMethods.quadsToString(SerializableMethods.tql, quad)))
        .toDF("dataset", "quads")
      try {
        quads.write.partitionBy("dataset").option("compression", "bzip2").text("/data/test_out/")
      } catch {
        case ex: Throwable => ex.printStackTrace()
      }

      //      // Retry failed pages
      //      if (retryFailedPages) {
      //            val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
      //            extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
      //            extractionRecorder.resetFailedPages(lang)
      //            val t = System.currentTimeMillis() // start timer
      //            sparkContext.parallelize(failedPages)
      //              .mapPartitions(pages => {
      //                //build new context with the broadcast values
      //                def worker_context = new DumpExtractionContext {
      //                  def ontology: Ontology = bc_ontology.value
      //                  def language: Language = bc_language.value
      //                  def redirects: Redirects = bc_redirects.value
      //                  def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
      //                }
      //                val localExtractor = CompositeParseExtractor.load(bc_extractors.value, worker_context)
      //                pages.map(SerializableMethods.processPage(bc_namespaces.value, localExtractor, _, extractionRecorder))
      //            })
      //              //execute transformations and collect results
      //              .toLocalIterator
      //              //write to destination and logging
      //              .foreach(result => {
      //                processResult(result, destination)
      //            })
      //        extractionRecorder.printLabeledLine(s"finished extraction of ${failedPages.length} failed pages with ~${System.currentTimeMillis() - t / failedPages.length} ms per page", RecordSeverity.Info, lang)
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


}

object SerializableMethods extends Serializable {
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

  def quadsToString(func: Quad => String, quad: Quad): String = {
    func(quad)
  }

  //  def record(records: Seq[RecordEntry[WikiPage]], extractionRecorder: ExtractionRecorder[WikiPage]): Unit = {
  //    extractionRecorder.record(records: _*)
  //    if (extractionRecorder.monitor != null) {
  //      records.filter(_.error != null).foreach(
  //        r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
  //      )
  //    }
  //  }
  def tql(quad: Quad) = {
    s"<${quad.subject}> <${quad.predicate}> ${'"'}${quad.value}${'"'}@${quad.language} <${quad.context}> ."
  }

  def ttl(quad: Quad) = {
    s"<${quad.subject}> <${quad.predicate}> ${'"'}${quad.value}${'"'}@${quad.language} ."
  }
}
