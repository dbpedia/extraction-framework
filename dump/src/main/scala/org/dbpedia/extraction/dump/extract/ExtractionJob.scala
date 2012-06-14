package org.dbpedia.extraction.dump.extract

import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.RootExtractor
import org.dbpedia.extraction.sources.{Source,WikiPage}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiParser}
import org.dbpedia.extraction.util.Runner

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
  
  def run(): Unit =
  {
    progress.start()
    
    destination.open()

    // If all slave threads are busy, the master thread will extract pages. But that means 
    // that it will not add pages to the queue for a while, so to make sure that the queue 
    // does not become empty, it must be large. Let's use 10 pages per thread. This should 
    // be enough even if the master happens to be processing a very large page while all 
    // other threads process very small pages.
    val runner = new Runner(10)
    
    for (page <- source) extractPage(runner, page)
    
    runner.shutdown()
    
    destination.close()
    
    progress.end()
  }
  
  private val parser = WikiParser()

  // Only extract from the following namespaces
  private val namespaces = Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template)

  private def extractPage(runner: Runner, page : WikiPage): Unit = {
    
    // If we use XMLSource, we probably checked this already, but anyway...
    if (! namespaces.contains(page.title.namespace)) return
    
    runner.run {
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
  }
    
}
