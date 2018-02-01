package org.dbpedia.extraction.dump.extract

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
class ExtractionJob( extractors: Seq[Class[_ <: Extractor[_]]],
                     context: DumpExtractionContext,
                     articleSource: Source,
                     namespaces: Set[Namespace],
                     destination: Destination,
                     lang: Language,
                     retryFailedPages: Boolean,
                     chunkSize: Int,
                     parallelProcesses: Int,
                     extractionRecorder: ExtractionRecorder[WikiPage]) {

  def run(sparkContext: SparkContext, config: Config): Unit = {
    try {
      //broadcasts
      val bc_namespaces = sparkContext.broadcast(namespaces)
      val bc_ontology = sparkContext.broadcast(context.ontology)
      val bc_language = sparkContext.broadcast(context.language)
      val bc_redirects = sparkContext.broadcast(context.redirects)
      val bc_mappingPageSource = sparkContext.broadcast(context.mappingPageSource)

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
      var chunkNumber = 1
      var readerFinished = false
      val pageBuffer = mutable.Queue[WikiPage]()
      val times = ListBuffer[Long]()

      // Start loading the WikiPages into memory
      Future {
        articleSource.foreach(page => {
          while (pageBuffer.lengthCompare(10 * chunkSize) >= 0) {
            Thread.sleep(1000)
          }
          pageBuffer.enqueue(page)
        })
      }.onComplete(_ => readerFinished = true)

      // Execute Extraction on the loaded WikiPages
      while (!readerFinished || pageBuffer.nonEmpty) {
        if (pageBuffer.lengthCompare(chunkSize) >= 0 || readerFinished) {
          // Set the size of the next chunk
          val nextChunkSize = if(pageBuffer.lengthCompare(chunkSize) > 0) chunkSize else pageBuffer.length
          // Start a timer
          val t = System.currentTimeMillis()
          // Queue[WikiPages] => RDD[WikiPage]
          sparkContext.parallelize((0 until nextChunkSize).map(_ => pageBuffer.dequeue), parallelProcesses)
          // WikiPage => Seq[Quad]
            .map(page => {
              try {
                // records
                val records = page.getExtractionRecords() match {
                  case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
                  case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
                }
                // extract, if in namespace
                if (bc_namespaces.value.exists(_.equals(page.title.namespace))) {
                  ExtractionResult(Some(bc_extractor.value.extract(page, page.uri)), records)
                }
                else {
                  ExtractionResult(None, records)
                }
              }
              catch {
                case ex: Exception =>
                  page.addExtractionRecord(null, ex)
                  ExtractionResult(None, page.getExtractionRecords())
              }
          })
          // Execute transformations and collect logs
            .toLocalIterator
          // Process Results & write Quads to Destination
            .foreach(results => {
              // write
              try {
                results.quads match {
                  case Some(quads) => destination.write(quads)
                  case None =>
                }
              } catch {
                case ex : Throwable =>
                  if (extractionRecorder.monitor != null) {
                    extractionRecorder.monitor.reportError(extractionRecorder, ex)
                  }
              }

              // logging
              val records = results.records
              extractionRecorder.record(records: _*)
              if (extractionRecorder.monitor != null) {
                records.filter(_.error != null).foreach(
                  r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
                )
              }
              // time
              times += (System.currentTimeMillis() - t) / nextChunkSize
              if (times.lengthCompare(1000) >= 0) {
                var average = 0f
                times.foreach(average += _)
                average /= times.length
                times.clear()
                extractionRecorder.printLabeledLine(s"finished extraction of chunk-group ${chunkNumber / 1000} with ~$average ms per page", RecordSeverity.Info, lang)
              }
              chunkNumber += 1
            })
        }
        else {
          Thread.sleep(100)
        }
      }
      // Retry failed pages
      if (retryFailedPages) {
            val failedPages = extractionRecorder.listFailedPages(lang).keys.map(_._2).toSeq
            extractionRecorder.printLabeledLine("retrying " + failedPages.length + " failed pages", RecordSeverity.Warning, lang)
            extractionRecorder.resetFailedPages(lang)
            val t = System.currentTimeMillis() // start timer
            sparkContext.parallelize(failedPages, parallelProcesses).map(page => {
              page.toggleRetry()
              try {
                //create records for this page
                val records = page.getExtractionRecords() match {
                  case seq: Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
                  case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
                }
                //extract quads, after checking the namespace
                if (bc_namespaces.value.exists(_.equals(page.title.namespace))) {
                  ExtractionResult(Some(bc_extractor.value.extract(page, page.uri)), records)
                }
                else {
                  ExtractionResult(None, records)
                }
              }
              catch {
                case ex: Exception =>
                  page.addExtractionRecord(null, ex)
                  ExtractionResult(None, page.getExtractionRecords())
              }
            })
              //execute transformations and collect results
              .toLocalIterator
              //write to destination and logging
              .foreach(results => {
                results.quads match {
                  case Some(quads) => destination.write(quads)
                  case None =>
                }
                val records = results.records
                extractionRecorder.record(records: _*)
                if (extractionRecorder.monitor != null) {
                  records.filter(_.error != null).foreach(
                    r => extractionRecorder.monitor.reportError(extractionRecorder, r.error)
                  )
                }
            })
        extractionRecorder.printLabeledLine(s"finished extraction of ${failedPages.length} failed pages with ~${System.currentTimeMillis() - t / failedPages.length} ms per page", RecordSeverity.Info, lang)
      }
      // Finish up Extraction
      extractionRecorder.printLabeledLine("finished complete extraction after {page} pages with {mspp} per page", RecordSeverity.Info, lang)
      destination.close()
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
}

case class ExtractionResult(quads : Option[Seq[Quad]], records : Seq[RecordEntry[WikiPage]]) extends Serializable


