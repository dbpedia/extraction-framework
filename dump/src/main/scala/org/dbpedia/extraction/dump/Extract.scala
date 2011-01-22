package org.dbpedia.extraction.dump

import java.util.logging.{Logger, FileHandler}
import java.io.File

/**
 * Dump extraction script.
 */
object Extract
{
    def main(args : Array[String]) : Unit =
    {
        val logHandler = new FileHandler("./log.xml")
        Logger.getLogger("org.dbpedia.extraction").addHandler(logHandler)

        val extraction = new ExtractionThread()
        extraction.start()
        extraction.join()
    }

    private class ExtractionThread extends Thread
    {
        override def run
        {
            val logger = Logger.getLogger("org.dbpedia.extraction");

            val configFile = new File("./config.properties");
            logger.info("Loading config from '" + configFile.getCanonicalPath + "'");

            //Load extraction jobs from configuration
            val extractionJobs = ConfigLoader.load(configFile)

            //Execute the extraction jobs one by one and print the progress to the console
            for(extractionJob <- extractionJobs)
            {
                if(isInterrupted) return

                extractionJob.start()

                try
                {
                    while(extractionJob.isAlive)
                    {
                        val progress = extractionJob.progress
                        if(progress.startTime > 0)
                        {
                            val time = (System.currentTimeMillis - progress.startTime).toDouble
                            println("Extracted " + progress.extractedPages + " pages (Per page: " + (time / progress.extractedPages) + " ms; Failed pages: " + progress.failedPages + ").")
                        }

                        Thread.sleep(1000L)
                    }
                }
                catch
                {
                    case _ : InterruptedException =>
                    {
                        println("Shutting down...")
                        extractionJob.interrupt()
                        extractionJob.join()
                        return
                    }
                }

            }
        }
    }
}
