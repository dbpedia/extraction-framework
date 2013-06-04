package org.dbpedia.extraction.live.job

import java.util.concurrent.ArrayBlockingQueue
import org.apache.log4j.Logger
import java.net.URLEncoder
import java.io.{InvalidClassException, File}
import scala.util.control.ControlThrowable
import org.dbpedia.extraction.sources.{Source, WikiPage}
import org.dbpedia.extraction.wikiparser.{WikiParser, Namespace}
import org.dbpedia.extraction.destinations.LiveDestination
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.RootExtractor

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 19, 2010
 * Time: 10:25:13 AM
 * To change this template use File | Settings | File Templates.
 */

// TODO: Change the behaviour of this file. After the Deletion of LiveUpdateDestination is does not work (not used

class LiveExtractionJob(extractor : RootExtractor, source : Source, language : Language, val label : String = "Extraction Job") extends Thread
{
    private val logger = Logger.getLogger(classOf[LiveExtractionJob].getName)

    private val parser = WikiParser.getInstance()
    var destination : LiveDestination = null;


    private val pageQueue = new ArrayBlockingQueue[(Int, WikiPage, LiveDestination)](20)

    private var currentID = 0

    override def run() : Unit =
    {
        logger.info(label + " started")

        val extractionJobs = for(_ <- 1 to 4) yield new ExtractionThread()

        try
        {

            //Start extraction jobs
            extractionJobs.foreach(_.start);

            //Start loading pages
            //println("Source Size = " + source.size);
            source.foreach(queuePage);
        }
        catch
        {
            case ex : ControlThrowable =>
            case ex : InterruptedException =>
            case ex => logger.fatal("Error reading pages. Shutting down...", ex)
        }
        finally
        {
            //Stop extraction jobs
          try{
            extractionJobs.foreach(_.interrupt);
            extractionJobs.foreach(_.join);

            logger.info(label + " finished");
          }
          catch{
            //case ex => logger.log(Level.SEVERE, "Error in step " + stepnumber + ", In thread "+
              //Thread.currentThread.getId + " Destination = " + destination.toString);
            case ex => logger.error("Error in thread number " + Thread.currentThread().getId +
              " and destination = " + destination);

          }

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
        {   // TODO comment for now, FIX later
            //destination = new LiveUpdateDestination(page.title.toString, language.locale.getLanguage, page.id.toString);
            //pageQueue.put((currentID, page, destination))
            currentID += 1
        }
        catch
        {
            case ex =>
            {
                logger.fatal("Inconsistet completion log. Shutting down...", ex)
                throw new RuntimeException with ControlThrowable
            }
        }
    }

    /**
     * An extraction thread.
     * Takes pages from a pageQueue and extracts them.
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

        private def extract(page : (Int, WikiPage, LiveDestination)) = extractPage(page._1, page._2, page._3)

        private def extractPage(id : Int, page : WikiPage, destination : LiveDestination) : Unit =
        {
            //Extract the page
            val success =
                try
                {
                  if(!destination.isInstanceOf[LiveDestination])
                    throw new InvalidClassException("Required LiveUpdateDestination class not passed");
                    val graph = extractor(parser(page));
                    //liveDest = new LiveUpdateDestination(CurrentPageNode.title.toString, language.locale.getLanguage(), CurrentPageNode.id.toString)
                    //var actualDestination = destination.asInstanceOf[LiveUpdateDestination];

                    destination.open();
                    destination.write(graph);
                    destination.close();
                    //_progress.synchronized(_progress.extractedPages +=1);
                    true
                }
                catch
                {
                    case ex : Exception =>
                    {
                        //_progress.synchronized(_progress.failedPages += 1)
                        logger.error("Destination = " + destination.toString);
                        logger.error("Error processing page '" + page.title + "'", ex)
                        false
                    }
                }

        }
    }
}