package org.dbpedia.extraction.server

import _root_.java.util.logging.{Level, Logger}
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.ResourceConfig
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

    val config = Configuration

    /**
     * The extraction manager
     * DynamicExtractionManager is able to update the ontology/mappings.
     * StaticExtractionManager is NOT able to update the ontology/mappings.
     */
    val extractor : ExtractionManager = new DynamicExtractionManager(config.languages, config.extractors)  // new StaticExtractionManager(languages, extractors)

    var adminRights : Boolean = false

    @volatile private var running = true

    def main(args : Array[String])
    {
        //Start the HTTP server
        val resources = new ClassNamesResourceConfig(
            classOf[Root], classOf[Extraction], classOf[Mappings], classOf[Ontology], classOf[Classes], classOf[Pages], classOf[Validate],
            classOf[TemplateStatistics], classOf[PropertyStatistics],
            classOf[XMLMessageBodyReader], classOf[XMLMessageBodyWriter], classOf[ExceptionMapper], classOf[TriX], classOf[Log], classOf[Percentage])
        
        // Jersey should redirect URLs like "/foo/../extractionSamples" to "/extractionSamples/" (with a slash at the end)
        val features = resources.getFeatures
        features.put(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, true)
        features.put(ResourceConfig.FEATURE_NORMALIZE_URI, true)
        features.put(ResourceConfig.FEATURE_REDIRECT, true)

        val server = HttpServerFactory.create(serverURI, resources)
        server.start()

        logger.info("Server started in " + System.getProperty("user.dir") + " listening on " + serverURI)

        //Open browser
        try
        {
            java.awt.Desktop.getDesktop.browse(serverURI)
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
