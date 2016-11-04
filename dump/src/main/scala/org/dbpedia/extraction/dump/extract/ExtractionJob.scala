package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.{ExtractionRecorder, WikiPageExtractor}
import org.dbpedia.extraction.sources.{Source, WikiPage}
import org.dbpedia.extraction.util.SimpleWorkers
import org.dbpedia.extraction.wikiparser.Namespace

/**
 * Executes a extraction.
 *
 * @param extractor The Extractor
 * @param source The extraction source
 * @param namespaces Only extract pages in these namespaces
 * @param destination The extraction destination. Will be closed after the extraction has been finished.
 * @param label user readable label of this extraction job.
 */
class ExtractionJob(extractor: WikiPageExtractor, source: Source, namespaces: Set[Namespace], destination: Destination, label: String, description: String, recorder: ExtractionRecorder)
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
        recorder.recordFailedPage(page.id, page.title, ex)
    }
  }
  
  def run(): Unit =
  {
    recorder.initialzeRecorder(label)

    extractor.initializeExtractor()
    
    destination.open()

    workers.start()

    for (page <- source) workers.process(page)

    workers.stop()
    
    destination.close()

    extractor.finalizeExtractor()
  }
}
