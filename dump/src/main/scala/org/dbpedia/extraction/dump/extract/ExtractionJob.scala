package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.{ExtractionRecorder, RecordEntry, WikiPageExtractor}
import org.dbpedia.extraction.sources.{Source, WikiPage}
import org.dbpedia.extraction.util.{Language, SimpleWorkers}
import org.dbpedia.extraction.wikiparser.Namespace

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
   namespaces: Set[Namespace],
   destination: Destination,
   language: Language,
   description: String,
   retryFailedPages: Boolean,
   extractionRecorder: ExtractionRecorder)
{
  private val workers = SimpleWorkers { page: WikiPage =>
    var success = false
    try {
      if (namespaces.contains(page.title.namespace)) {
        //val graph = extractor(parser(page))
        val graph = extractor.extract(page, page.uri)
        destination.write(graph)
      }
      success = true
      val records = page.getExtractionRecords() match{
        case seq :Seq[RecordEntry] if seq.nonEmpty => seq
        case _ => Seq(new RecordEntry(page))
      }
      extractionRecorder.record(records:_*)
    } catch {
      case ex: Exception =>
        page.addExtractionRecord(null, ex)
        extractionRecorder.record(page.getExtractionRecords():_*)
    }
  }
  
  def run(): Unit =
  {
    extractionRecorder.initialzeRecorder(language.wikiCode)

    extractor.initializeExtractor()
    
    destination.open()

    workers.start()

    for (page <- source)
      workers.process(page)

    extractionRecorder.printLabeledLine("finished extraction after {page} pages with {mspp} per page", language)

    if(retryFailedPages){
      val fails = extractionRecorder.listFailedPages.get(language).get.keys.map(_._2)
      extractionRecorder.printLabeledLine("retrying " + fails.size + " failed pages", language)
      extractionRecorder.resetFailedPages(language)
      for(page <- fails) {
        page.toggleRetry()
        page match{
          case p: WikiPage => workers.process(p)
          case _ =>
        }
      }
      extractionRecorder.printLabeledLine("all failed pages were retried.", language)
    }

    workers.stop()
    
    destination.close()

    extractor.finalizeExtractor()
  }
}
