package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.{ExtractionRecorder, WikiPageExtractor}
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
   recorder: ExtractionRecorder)
{
  private val workers = SimpleWorkers { page: WikiPage =>
    var success = false
    try {
      if (namespaces.contains(page.title.namespace)) {
        //val graph = extractor(parser(page))
        val graph = extractor.extract(page)
        destination.write(graph)
      }
      success = true
      recorder.recordExtractedPage(page.id, page.title)
    } catch {
      case ex: Exception =>
        recorder.recordFailedPage(page.id, page, ex)
    }
  }
  
  def run(): Unit =
  {
    recorder.initialzeRecorder(language.wikiCode)

    extractor.initializeExtractor()
    
    destination.open()

    workers.start()

    for (page <- source)
      workers.process(page)

    if(retryFailedPages){
      val fails = recorder.listFailedPages.get(language).get.keys.map(_._2)
      recorder.resetFailedPages(language)
      for(page <- fails) {
        page.setRetry(true)
        workers.process(page)
      }
    }

    workers.stop()
    
    destination.close()

    extractor.finalizeExtractor()
  }
}
