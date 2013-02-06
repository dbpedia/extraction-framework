package org.dbpedia.extraction.dump.extract

import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.RootExtractor
import org.dbpedia.extraction.sources.{Source,WikiPage}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiParser}
import org.dbpedia.extraction.util.SimpleWorkers

/**
 * Executes a extraction.
 *
 * @param extractor The Extractor
 * @param source The extraction source
 * @param namespaces Only extract pages in these namespaces
 * @param destination The extraction destination. Will be closed after the extraction has been finished.
 * @param label user readable label of this extraction job.
 */
class ExtractionJob(extractor: RootExtractor, source: Source, namespaces: Set[Namespace], destination: Destination, label: String, description: String)
{
  private val logger = Logger.getLogger(getClass.getName)

  private val progress = new ExtractionProgress(label, description)
  
  private val parser = WikiParser()

  private val workers = SimpleWorkers { page: WikiPage =>
    var success = false
    try {
      if (namespaces.contains(page.title.namespace)) {
        val graph = extractor(parser(page))
        destination.write(graph)
      }
      success = true
    } catch {
      case ex: Exception => logger.log(Level.WARNING, "error processing page '"+page.title+"'", ex)
    }
    progress.countPage(success)
  }
  
  def run(): Unit =
  {
    progress.start()
    
    destination.open()

    workers.start()
    
    for (page <- source) workers.process(page)
    
    workers.stop()
    
    destination.close()
    
    progress.end()
  }
  
}
