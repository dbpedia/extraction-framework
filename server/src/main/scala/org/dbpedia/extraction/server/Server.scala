package org.dbpedia.extraction.server

import java.io.File
import java.net.{URI,URL}
import java.util.logging.{Level,Logger}
import scala.collection.mutable
import org.dbpedia.extraction.mappings.{LabelExtractor,MappingExtractor}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.server.util.MappingStatsManager
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{ResourceConfig,PackagesResourceConfig}
import org.dbpedia.extraction.util.StringUtils.prettyMillis

/**
 * The DBpedia server.
 * FIXME: more flexible configuration.
 */
object Server
{
    val logger = Logger.getLogger(getClass.getName)

    // Note: we use /server/ because that's what we use on http://mappings.dbpedia.org/server/
    // It makes handling redirects easier.
    val serverURI = new URI("http://localhost:9999/server/")

    def languages = _languages
    private var _languages : Seq[Language] = null

    /**
     * The extraction manager
     * DynamicExtractionManager is able to update the ontology/mappings.
     * StaticExtractionManager is NOT able to update the ontology/mappings.
     */
    def extractor : ExtractionManager = _extractor
    private var _extractor : ExtractionManager = null

    private var _managers : Map[Language, MappingStatsManager] = null
    
    def statsManager(language: Language) : MappingStatsManager = _managers(language)

    private var _password : String = null
    
    def adminRights(pass : String) : Boolean = _password == pass

    /** The URL where the pages of the Mappings Wiki are located */
    val wikiPagesUrl = new URL("http://mappings.dbpedia.org/index.php")

    /** The URL of the MediaWiki API of the Mappings Wiki */
    val wikiApiUrl = new URL("http://mappings.dbpedia.org/api.php")
    
    def main(args : Array[String])
    {
        val millis = System.currentTimeMillis
        
        logger.info("DBpedia server starting")
        
        require(args != null && args.length >= 5, "need at least five args: password for template ignore list, base dir for statistics, ontology file, mappings dir, languages")
        _password = args(0)
        val statsDir = new File(args(1))

        val ontologyFile = new File(args(2))
        val mappingsDir = new File(args(3))
        
        // Use all remaining args as language codes or comma or whitespace separated lists of codes
        _languages = for(arg <- args.drop(4); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield Language(lang)
        
        _managers = _languages.map(language => (language -> new MappingStatsManager(statsDir, language))).toMap
        
        _extractor = new DynamicExtractionManager(_languages, List(classOf[LabelExtractor],classOf[MappingExtractor]), ontologyFile, mappingsDir)
        
        // Configure the HTTP server
        val resources = new PackagesResourceConfig("org.dbpedia.extraction.server.resources", "org.dbpedia.extraction.server.providers")
        
        // redirect URLs like "/foo/../extractionSamples" to "/extractionSamples/" (with a slash at the end)
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
        
        logger.info("DBpedia server started in "+prettyMillis(System.currentTimeMillis - millis))
    }
}
