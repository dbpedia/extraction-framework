package org.dbpedia.extraction.wiktionary

import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.RootExtractor
import org.dbpedia.extraction.sources.{Source, WikiPage}
import org.dbpedia.extraction.wikiparser.{WikiTitle, WikiParser, Namespace}
import java.util.concurrent.{ArrayBlockingQueue}
import java.util.logging.{Level, Logger}
import scala.util.control.ControlThrowable
import java.io.File
import java.net.URLEncoder

/**
 * Executes a extraction.
 *
 * @param extractor The Extractor
 * @param source The extraction source
 * @param destination The extraction destination. Will be closed after the extraction has been finished.
 * @param label A user readable label of this extraction job
 */
class ExtractionJob(extractor : RootExtractor, source : Source, destination : Destination, val label : String = "Extraction Job") extends Thread
{
    private val logger = Logger.getLogger(classOf[ExtractionJob].getName)

    private val parser = WikiParser()

    private val _progress = new ExtractionProgress()

    def progress = _progress

    private val pageQueue = new ArrayBlockingQueue[(Int, WikiPage)](20)

    private val completionReader = new CompletionReader(new File("./" + URLEncoder.encode(label, "UTF-8")))

    private val completionWriter = new CompletionWriter(new File("./" + URLEncoder.encode(label, "UTF-8") + ".tmp"))

    private var currentID = 0

    override def run() : Unit =
    {
        logger.info(label + " started")

        val extractionJobs = for(_ <- 1 to 4) yield new ExtractionThread()

        try
        {
            _progress.startTime = System.currentTimeMillis

            //Start extraction jobs
            extractionJobs.foreach(_.start)

            //Start loading pages
            source.foreach(queuePage)
        }
        catch
        {
            case ex : ControlThrowable =>
            case ex : InterruptedException =>
            case ex => logger.log(Level.SEVERE, "Error reading pages. Shutting down...", ex)
        }
        finally
        {
            //Stop extraction jobs
            extractionJobs.foreach(_.interrupt)
            extractionJobs.foreach(_.join)

            completionWriter.close()
            destination.close()

            logger.info(label + " finished")
        }
    }

    private def queuePage(page : WikiPage)
    {
        //Only extract from the following namespaces
        if(page.title.namespace != Namespace.Main &&
           page.title.namespace != Namespace.File &&
           page.title.namespace != Namespace.Category)
        {
           return 
        }

        try
        {
            val done = completionReader.read(currentID, page.title)
            if(!done) pageQueue.put((currentID, page))
            currentID += 1
        }
        catch
        {
            case ex =>
            {
                logger.log(Level.SEVERE, "Inconsistet completion log. Shutting down...", ex)
                throw new RuntimeException with ControlThrowable
            }
        }
    }

    /**
     * An extraction thread.
     * Takes pages from a queue and extracts them.
     */
    private class ExtractionThread() extends Thread
    {
        @volatile private var running = true

        override def interrupt()
        {
            running = false
        }

        override def run() : Unit =
        {
            //Extract remaining pages
            while(running || !pageQueue.isEmpty)
            {
                val page = pageQueue.poll
                if(page != null)
                {
                    extract(page)
                }
                else
                {
                    Thread.sleep(10)
                }
            }
        }

        private def extract(page : (Int, WikiPage)) = extractPage(page._1, page._2) 

        private def extractPage(id : Int, page : WikiPage) : Unit =
        {
            //Extract the page
            val success =
                try
                {
                    val graph = extractor(parser(page))
                    destination.write(graph)
                    _progress.synchronized(_progress.extractedPages +=1)

                    true
                }
                catch
                {
                    case ex : Exception =>
                    {
                        _progress.synchronized(_progress.failedPages += 1)
                        logger.log(Level.WARNING, "Error processing page '" + page.title + "'", ex)
                        false
                    }
                }

            //Write the extraction success
            completionWriter.write(id, page.title, success)
        }
    }
}