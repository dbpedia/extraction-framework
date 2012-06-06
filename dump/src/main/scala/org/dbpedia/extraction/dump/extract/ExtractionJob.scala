package org.dbpedia.extraction.dump.extract

import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.RootExtractor
import org.dbpedia.extraction.sources.{Source,WikiPage}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiParser}
import java.util.concurrent.{ExecutorService,ThreadPoolExecutor,ArrayBlockingQueue,TimeUnit}

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
  
  private def createExecutor(): ExecutorService = {
  
    // We want to distribute work to several threads, without complex locks, waits and signals. 
    // In JDK 6, this seems to be the simplest way. In JDK 7, we might use fork-join.
    // TODO: Maybe Scala actors would be more concise?
    
    // availableProcessors returns logical processors, not physical
    val cpus = Runtime.getRuntime.availableProcessors
    
    // The following is similar to Executors.newFixedThreadPool(cpus), but with added features.
    // CallerRunsPolicy means that if all threads are busy, the master thread will extract pages. 
    // But that means that it will not add pages to the queue for a while, so to make sure that
    // the queue does not become empty, it must be large. Let's use 10 pages per thread. This
    // should be enough even if the master happens to be processing a very large page while all
    // other threads process very small pages.
    // It would be nice to re-use the pool across ExtractionJob instances, but shutdown seems 
    // to be the only way to wait for all currently working threads to finish. 
    
    val policy = new ThreadPoolExecutor.CallerRunsPolicy()
    val queue = new ArrayBlockingQueue[Runnable](cpus * 10)
    
    new ThreadPoolExecutor(cpus, cpus, 0L, TimeUnit.MILLISECONDS, queue, policy)
  }
    
  private def shutdownExecutor(pool: ExecutorService): Unit = {
    pool.shutdown()
    while (! pool.awaitTermination(1L, TimeUnit.MINUTES)) {
      // should never happen
      logger.log(Level.SEVERE, "extraction did not terminate - waiting one more minute")
    }
  }
  
  def run(): Unit =
  {
    progress.start()
    
    destination.open()

    val executor = createExecutor()
    
    for (page <- source) extractPage(executor, page)
    
    shutdownExecutor(executor)
    
    destination.close()
    
    progress.end()
  }
  
  // Only extract from the following namespaces
  private val namespaces = Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template)

  private def extractPage(executor: ExecutorService, page : WikiPage)
  {
    // If we use XMLSource, we probably checked this already, but anyway...
    if (! namespaces.contains(page.title.namespace)) return
    
    executor.execute(new Runnable() { def run() { extractPage(page) } } )
  }
  
  private val parser = WikiParser()

  private def extractPage(page : WikiPage)
  {
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
