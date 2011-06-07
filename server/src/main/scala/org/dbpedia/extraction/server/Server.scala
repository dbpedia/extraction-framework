package org.dbpedia.extraction.server

import _root_.java.util.logging.{Level, Logger}
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.ClassNamesResourceConfig
import providers._
import resources._
import ontology._
import stylesheets.{Log, TriX}
import java.net.URI

/**
 * The DBpedia server.
 */
object Server
{
    val serverURI = new URI("http://localhost:9999/")

    val logger = Logger.getLogger(Server.getClass.getName)

   //@volatile var currentJob : Option[ExtractionJob] = None

    val config = new Configuration()

    val extractor = new ExtractionManager(config.languages)

    @volatile private var running = true

    def main(args : Array[String]) : Unit =
    {
        //Start the HTTP server
        val resources = new ClassNamesResourceConfig(classOf[Root], classOf[Extraction], classOf[Mappings],
            classOf[Ontology], classOf[Classes], classOf[Pages], classOf[Validate],
            classOf[XMLMessageBodyReader], classOf[XMLMessageBodyWriter], classOf[ExceptionMapper], classOf[TriX], classOf[Log])

        val server = HttpServerFactory.create(serverURI, resources)
        server.start()

        logger.info("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI)

        //Open browser
        try
        {
            java.awt.Desktop.getDesktop().browse(serverURI)
        }
        catch
        {
            case ex : Exception => logger.log(Level.WARNING, "Could not open browser.", ex)
        }

        /*
        //Load Extraction jobs from configuration
        val extractionJobs = ConfigLoader.load(new File("./config.properties"))

        //Execute the Extraction jobs one by one
        for(extractionJob <- extractionJobs)
        {
           currentJob = Some(extractionJob)
           extractionJob.start()
           extractionJob.join()
        }
        currentJob = None
        */

        while(running) Thread.sleep(100)

        //Stop the HTTP server
        server.stop(0)
   }
}
