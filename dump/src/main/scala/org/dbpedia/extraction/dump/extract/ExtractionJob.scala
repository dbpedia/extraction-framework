package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.WikiPageExtractor
import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity}
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiPage}

/**
  * Executes a extraction.
  *
  * @param extractor The Extractor
  * @param source The extraction source
  * @param namespaces Only extract pages in these namespaces
  * @param destination The extraction destination. Will be closed after the extraction has been finished.
  * @param language the language of this extraction.
  */
class ExtractionJob(
                     extractor: WikiPageExtractor,
                     source: Source,
                     val namespaces: Set[Namespace],
                     val destination: Destination,
                     val language: Language,
                     val retryFailedPages: Boolean,
                     val extractionRecorder: ExtractionRecorder[WikiPage])
{
  /*  val myAnnotatedClass: ClassSymbol = runtimeMirror(Thread.currentThread().getContextClassLoader).classSymbol(ExtractorAnnotation.getClass)
    val annotation: Option[Annotation] = myAnnotatedClass.annotations.find(_.tree.tpe =:= typeOf[ExtractorAnnotation])
    val result = annotation.flatMap { a =>
      a.tree.children.tail.collect({ case Literal(Constant(name: String)) => name }).headOption
    }

    result.foreach( x => println(x.toString))*/

  def datasets: Set[Dataset] = extractor.datasets

  private val workers = SimpleWorkers { page: WikiPage =>
    try {
      if (namespaces.contains(page.title.namespace)) {
        val graph = extractor.extract(page, page.uri)
        destination.write(graph)
      }
      //if the internal extraction process of this extractor yielded extraction records (e.g. non critical errors etc.), those will be forwarded to the ExtractionRecorder, else a new record is produced
      val records = page.getExtractionRecords() match{
        case seq :Seq[RecordEntry[WikiPage]] if seq.nonEmpty => seq
        case _ => Seq(new RecordEntry[WikiPage](page, page.uri, RecordSeverity.Info, page.title.language))
      }
      //forward all records to the recorder
      extractionRecorder.record(records:_*)
    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        page.addExtractionRecord(null, ex)
        extractionRecorder.record(page.getExtractionRecords():_*)
        if(extractionRecorder.monitor != null)
          extractionRecorder.monitor.reportError(extractionRecorder, ex)
    }
  }

  def run(): Unit =
  {
    extractionRecorder.initialize(language, "Main Extraction", extractor.datasets.toSeq)
    extractor.initializeExtractor()
    destination.open()
    workers.start()

    try {
      for (page <- source)
        workers.process(page)

      extractionRecorder.printLabeledLine("finished extraction after {page} pages with {mspp} per page", RecordSeverity.Info, language)

      if(retryFailedPages){
        val fails = extractionRecorder.listFailedPages(language).keys.map(_._2)
        extractionRecorder.printLabeledLine("retrying " + fails.size + " failed pages", RecordSeverity.Warning, language)
        extractionRecorder.resetFailedPages(language)
        for(page <- fails) {
          page.toggleRetry()
          page match{
            case p: WikiPage => workers.process(p)
            case _ =>
          }
        }
        extractionRecorder.printLabeledLine("all failed pages were re-executed.", RecordSeverity.Info, language)
      }
    }
    catch {
      case ex : Throwable =>
        if(extractionRecorder.monitor != null) extractionRecorder.monitor.reportCrash(extractionRecorder, ex)
    } finally {
      workers.stop()
      destination.close()
      extractor.finalizeExtractor()
      extractionRecorder.finalize()
    }
  }
}