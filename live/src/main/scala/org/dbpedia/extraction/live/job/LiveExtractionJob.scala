package org.dbpedia.extraction.live.job

import _root_.org.dbpedia.extraction.destinations.Destination
import _root_.org.dbpedia.extraction.mappings.Extractor
import _root_.org.dbpedia.extraction.sources.{Source, WikiPage}
import _root_.org.dbpedia.extraction.wikiparser.{WikiTitle, WikiParser, Namespace}
import java.util.concurrent.{ArrayBlockingQueue}
import java.util.logging.{Level, Logger}
import scala.util.control.ControlThrowable
import java.net.URLEncoder
import org.dbpedia.extraction.live.destinations.LiveUpdateDestination
import java.io.{InvalidClassException, File}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.RootExtractor

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 19, 2010
 * Time: 10:25:13 AM
 * To change this template use File | Settings | File Templates.
 */

class LiveExtractionJob(extractor : RootExtractor, source : Source, language : Language, val label : String = "Extraction Job") extends Thread
{
    private val logger = Logger.getLogger(classOf[LiveExtractionJob].getName)

    private val parser = WikiParser()
    var destination : LiveUpdateDestination = null;

    //private val _progress = new ExtractionProgress()

    //def progress = _progress

    private val pageQueue = new ArrayBlockingQueue[(Int, WikiPage, LiveUpdateDestination)](20)

    private var currentID = 0

    override def run() : Unit =
    {
        logger.info(label + " started")

        val extractionJobs = for(_ <- 1 to 4) yield new ExtractionThread()

        try
        {
            //_progress.startTime = System.currentTimeMillis

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
            case ex => logger.log(Level.SEVERE, "Error reading pages. Shutting down...", ex)
        }
        finally
        {
            //Stop extraction jobs
          try{
            extractionJobs.foreach(_.interrupt);
            extractionJobs.foreach(_.join);
            //completionWriter.close();
//            System.out.println("Before close in thread " + Thread.currentThread.getId);
//            if(destination == null)
//              destination = new LiveUpdateDestination(page.title.toString, language.locale.getLanguage, page.id.toString);
//
//            destination.close();
//            System.out.println("After close in thread " + Thread.currentThread.getId);
            logger.info(label + " finished");
          }
          catch{
            //case ex => logger.log(Level.SEVERE, "Error in step " + stepnumber + ", In thread "+
              //Thread.currentThread.getId + " Destination = " + destination.toString);
            case ex => logger.log(Level.SEVERE, "Error in thread number " + Thread.currentThread().getId +
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
        {
            destination = new LiveUpdateDestination(page.title.toString, language.locale.getLanguage, page.id.toString);
            pageQueue.put((currentID, page, destination))
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

        private def extract(page : (Int, WikiPage, LiveUpdateDestination)) = extractPage(page._1, page._2, page._3)

        private def extractPage(id : Int, page : WikiPage, destination : LiveUpdateDestination) : Unit =
        {
            //Extract the page
            val success =
                try
                {
                  if(!destination.isInstanceOf[LiveUpdateDestination])
                    throw new InvalidClassException("Required LiveUpdateDestination class not passed");
                    val graph = extractor(parser(page));
                    //liveDest = new LiveUpdateDestination(CurrentPageNode.title.toString, language.locale.getLanguage(), CurrentPageNode.id.toString)
                    //var actualDestination = destination.asInstanceOf[LiveUpdateDestination];

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
                        logger.log(Level.SEVERE, "Destination = " + destination.toString);
                        logger.log(Level.SEVERE, "Error processing page '" + page.title + "'", ex)
                        false
                    }
                }

            //Write the extraction success
            //completionWriter.write(id, page.title, success)
        }
    }
}