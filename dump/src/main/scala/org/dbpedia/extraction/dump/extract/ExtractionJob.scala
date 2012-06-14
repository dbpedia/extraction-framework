package org.dbpedia.extraction.dump.extract

import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.RootExtractor
import org.dbpedia.extraction.sources.{Source,WikiPage}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiParser}
import org.dbpedia.extraction.util.Workers

/**
 * Executes a extraction.
 *
 * @param extractor The Extractor
 * @param source The extraction source
 * @param destination The extraction destination. Will be closed after the extraction has been finished.
 * @param label user readable label of this extraction job.
 */
class ExtractionJob(extractor: RootExtractor, source: Source, destination: Destination, label: String)
{
  private val logger = Logger.getLogger(getClass.getName)

  private val progress = new ExtractionProgress(label)
  
  private val parser = WikiParser()

  // Only extract from the following namespaces
  private val namespaces = Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template)

  def run(): Unit =
  {
    progress.start()
    
    destination.open()

    val workers = Workers { page: WikiPage =>
      var success = false
      try {
        val graph = extractor(parser(page))
        destination.write(graph)
        success = true
      } catch {
        case ex: Exception => logger.log(Level.WARNING, "error processing page '"+page.title+"'", ex)
      }
      progress.countPage(success)
    }
    
    workers.start()
    
    for (page <- source) {
      // If we use XMLSource, we probably checked this already, but anyway...
      if (namespaces.contains(page.title.namespace)) {
        workers.process(page)
      }
    }
    
    workers.stop()
    
    destination.close()
    
    progress.end()
  }
  
}
