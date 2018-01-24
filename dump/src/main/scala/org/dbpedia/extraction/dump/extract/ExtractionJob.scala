package org.dbpedia.extraction.dump.extract

import org.apache.log4j.LogManager
import org.apache.spark.SparkContext
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{RecordSeverity, _}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable

/**
  * Executes a extraction.
  *
  * @param extractors The Extractors
  * @param context    The Extraction Context
  * @param namespaces Only extract pages in these namespaces
  * @param lang       the language of this extraction.
  */
class ExtractionJob(
                     extractors: Seq[Class[_ <: Extractor[_]]],
                     context: DumpExtractionContext,
                     articleSource: Source,
                     namespaces: Set[Namespace],
                     destination: Destination,
                     lang: Language,
                     retryFailedPages: Boolean,
                     extractionRecorder: ExtractionRecorder[WikiPage]) {

  val logger = LogManager.getRootLogger()

  def run(sparkContext: SparkContext, config: Config): Unit = {
    val chunkSize = 100

    //broadcasts
    val bc_namespaces = sparkContext.broadcast(namespaces)
    val bc_ontology = sparkContext.broadcast(context.ontology)
    val bc_language = sparkContext.broadcast(context.language)
    val bc_redirects = sparkContext.broadcast(context.redirects)
    val bc_mappingPageSource = sparkContext.broadcast(context.mappingPageSource)
    val bc_destination = sparkContext.broadcast(destination)

    //build new context with the broadcast values
    def worker_context = new DumpExtractionContext {
      def ontology: Ontology = bc_ontology.value

      def language: Language = bc_language.value

      def redirects: Redirects = bc_redirects.value

      def mappingPageSource: Traversable[WikiPage] = bc_mappingPageSource.value
    }

    //create extractors
    val extractor = CompositeParseExtractor.load(extractors, worker_context)
    val bc_extractor = sparkContext.broadcast(extractor)
    //initialize extraction
    extractionRecorder.initialize(lang, sparkContext.appName, extractor.datasets.toSeq)
    extractor.initializeExtractor()
    extractionRecorder.printLabeledLine(s"starting extraction with a max chunk-size of $chunkSize pages", RecordSeverity.Info, lang)
    destination.open()
    val pages = ListBuffer[WikiPage]()
    var chunkNumber = 1
    var prodFinished = false

    val pageQueue = mutable.Queue[WikiPage]()
    val times = ListBuffer[Long]()

    Future {
      articleSource.foreach(page => {
        while (pageQueue.length >= 8 * chunkSize) {
          Thread.sleep(1000)
        }
        pageQueue.enqueue(page)

      })
    }.onComplete(_ => prodFinished = true)

    while (!(prodFinished && pageQueue.isEmpty)) {
      if (!prodFinished) {
        if (pageQueue.length >= chunkSize) {
          val t = System.currentTimeMillis()
          sparkContext.parallelize((0 until chunkSize).map(_ => pageQueue.dequeue()), 8).map(page => {
            try {
              //create records for this page
              val records = page.getExtractionRecords() match {
                case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
                case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
              }
              //extract quads, after checking the namespace
              if (bc_namespaces.value.exists(_.equals(page.title.namespace))) {
                bc_destination.value.write(bc_extractor.value.extract(page, page.uri))
                records
              }
              else {
                records
              }
            }
            catch {
              case ex: Exception =>
                page.addExtractionRecord(null, ex)
                page.getExtractionRecords()
            }
          })
            //execute transformations and collect results
            .toLocalIterator
            //write to destination and logging
            .foreach(records => {
            extractionRecorder.record(records: _*)
            if (extractionRecorder.monitor != null) {
              records.filter(_.error != null).foreach(
                r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
              )
            }
            times += (System.currentTimeMillis() - t) / chunkSize
            if(times.length >= 1000) {
              var median = 0f
              times.foreach(median += _)
              median /= times.length
              times.clear()
              extractionRecorder.printLabeledLine(s"finished extraction of chunk ${chunkNumber/1000} with ~${median} ms per page", RecordSeverity.Info, lang)
            }
            chunkNumber+=1
          })
        } else {
          Thread.sleep(10)
        }
      } else {
        if (pageQueue.nonEmpty) {
          /* Last Chunk & Retry */
        }
          extractionRecorder.printLabeledLine(s"finished extraction with {mspp} per page", RecordSeverity.Info, lang)
        }
      }
      //
      //    //extraction
      //    try {
      //      //split input data in chunks
      //      articleSource.foreach(wikiPage => {
      //        if (pages.lengthCompare(chunkSize) < 0) {
      //          pages += wikiPage
      //        } else {
      //          extractionRecorder.printLabeledLine(s"extracting chunk $chunkNumber with a size of " + pages.length, RecordSeverity.Info, lang)
      //          //define transformations for the pages
      //          val source = sparkContext.parallelize(pages, numSlices = chunkSize)
      //          source
      //          pages.clear()
      //          pages += wikiPage
      //        }
      //      })
      //      //extract last chunk
      //      extractionRecorder.printLabeledLine(s"extracting chunk $chunkNumber with a size of " + pages.length, RecordSeverity.Info, lang)
      //      val source = sparkContext.parallelize(pages)
      //      //define transformations for the pages
      //      source.map(page => {
      //        try {
      //          //create records for this page
      //          val records = page.getExtractionRecords() match {
      //            case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
      //            case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
      //          }
      //          //extract quads, after checking the namespace
      //          if (bc_namespaces.value.exists(_.equals(page.title.namespace))) {
      //            val quads = bc_extractor.value.extract(page, page.uri)
      //            new ExtractionResult(Some(quads), records)
      //          }
      //          else {
      //            new ExtractionResult(None, records)
      //          }
      //        }
      //        catch {
      //          case ex: Exception =>
      //            page.addExtractionRecord(null, ex)
      //            new ExtractionResult(None, page.getExtractionRecords())
      //        }
      //      })
      //        //execute transformations and collect results
      //        .collect()
      //        .foreach(results => {
      //          //write quads to the destination
      //          results.quads.foreach(destination.write)
      //          //push records to ExtractionRecorder
      //          val records = results.records
      //          extractionRecorder.record(records: _*)
      //          //report exceptions to the monitor
      //          if (extractionRecorder.monitor != null) {
      //            records.filter(_.error != null).foreach(
      //              r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
      //            )
      //          }
      //        })
      //
      //      extractionRecorder.printLabeledLine(s"finished extraction of chunk $chunkNumber with {mspp} per page", RecordSeverity.Info, lang)
      //
      //      // TODO: Retry Extraction of failed pages
      //      if (false && retryFailedPages) {
      //        val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
      //
      //        // Failed Pages -> SparkRDD[WikiPage]
      //        val fails = sparkContext.parallelize(failedPages)
      //        extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
      //        extractionRecorder.resetFailedPages(lang)
      //
      //        // Rerun Extraction on SparkRDD
      //        fails.map(page => {
      //          page.toggleRetry()
      //          try {
      //            // Create records for this page
      //            val records = page.getExtractionRecords() match {
      //              case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
      //              case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
      //            }
      //            // extract quads if the namespace is requested
      //            if (bc_namespaces.value.exists(_.equals(page.title.namespace))) {
      //              val quads = bc_extractor.value.extract(page, page.uri)
      //              new ExtractionResult(Some(quads), records)
      //            }
      //            else {
      //              new ExtractionResult(None, records)
      //            }
      //          }
      //          catch {
      //            case ex: Exception =>
      //              page.addExtractionRecord(null, ex)
      //              new ExtractionResult(None, page.getExtractionRecords())
      //          }
      //        })
      //          // Collect results
      //          .collect()
      //          .foreach(results => {
      //            // Write quads to the destination
      //            results.quads match {
      //              case Some(quads) =>
      //                quads.foreach(q => println("s: " + q.subject + "\np: " + q.predicate + "\no: " + q.value + "\n\n"))
      //                destination.write(quads)
      //              case None =>
      //            }
      //            // Push records to ExtractionRecorder
      //            val records = results.records
      //            extractionRecorder.record(records: _*)
      //            // Report exceptions to the monitor
      //            if (extractionRecorder.monitor != null) {
      //              records.filter(_.error != null).foreach(
      //                r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
      //              )
      //            }
      //          })
      //        extractionRecorder.printLabeledLine("all failed pages were re-executed.", RecordSeverity.Info, lang)
      //      }
      //    }
      //    catch {
      //      case ex: Throwable =>
      //        if (extractionRecorder.monitor != null) extractionRecorder.monitor.reportCrash(extractionRecorder, ex)
      //        ex.printStackTrace()
      //    }

      extractionRecorder.printLabeledLine("finished complete extraction after {page} pages with {mspp} per page", RecordSeverity.Info, lang)
      destination.close()
      extractor.finalizeExtractor()
      extractionRecorder.finalize()
    }

}

/**
  * result object for the extraction process
  * @param quads generated quads
  * @param records generated records for the ExtractionRecorder
  */
class ExtractionResult(val quads: Option[Seq[Quad]], val records: Seq[RecordEntry[WikiPage]]) extends Serializable
