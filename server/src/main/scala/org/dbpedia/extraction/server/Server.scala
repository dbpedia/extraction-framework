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
import java.io.File

/**
 * The DBpedia server.
 */
object Server
{
    // Note: we use /server/ because that's what we use on http://mappings.dbpedia.org/server/
    // It makes handling redirects easier.
    val serverURI = new URI("http://localhost:9999/server/")

    val logger = Logger.getLogger(Server.getClass.getName)

    //@volatile var currentJob : Option[ExtractionJob] = None

    val config = Configuration
    
    /**
     * The extraction manager
     * DynamicExtractionManager is able to update the ontology/mappings.
     * StaticExtractionManager is NOT able to update the ontology/mappings.
     */
    def extractor : ExtractionManager = _extractor

    private var _extractor : ExtractionManager = null

    private var password : String = null

    def adminRights(pass : String) : Boolean = password == pass

    @volatile private var running = true

    def main(args : Array[String])
    {
        require(args != null && args.length >= 1, "need password for template ignore list")
        require(args(0).length >= 4, "password for template ignore list must be at least four characters long, got ["+args(0)+"]")
        
        password = args(0)

        var ontologyFile = if (args.length >= 2 && args(1).nonEmpty) new File(args(1)) else null
        var mappingsDir = if (args.length >= 3 && args(2).nonEmpty) new File(args(2)) else null
        
        _extractor = new DynamicExtractionManager(config.languages, config.extractors, ontologyFile, mappingsDir)
        
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
