package org.dbpedia.extraction.server

import java.io.File
import java.net.{URI,URL}
import java.util.logging.{Level,Logger}
import scala.collection.immutable.SortedMap
import org.dbpedia.extraction.mappings.{Redirects, LabelExtractor, MappingExtractor, Mappings}
import org.dbpedia.extraction.util.{ConfigUtils, Language}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.server.stats.MappingStatsManager
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{ResourceConfig,PackagesResourceConfig}
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.{WikiTitle, Namespace}
import Server._

class Server(private val password : String, langs : Seq[Language], val paths: Paths) 
{
    val managers = {
      val tuples = langs.map(lang => lang -> new MappingStatsManager(paths.statsDir, lang))
      SortedMap(tuples: _*)
    }

    val redirects = {
      managers.map(manager => (manager._1, buildTemplateRedirects(manager._2.wikiStats.redirects, manager._1))).toMap
    }
        
    val extractor: ExtractionManager = new DynamicExtractionManager(managers(_).updateStats(_), langs, paths, redirects)
    
    extractor.updateAll
        
    def adminRights(pass : String) : Boolean = password == pass
}

/**
 * The DBpedia server.
 * FIXME: more flexible configuration.
 */
object Server
{
    val logger = Logger.getLogger(getClass.getName)

    private var _instance: Server = null
    
    def instance = _instance
    
    def main(args : Array[String])
    {
        val millis = System.currentTimeMillis
        
        logger.info("DBpedia server starting")
        
        require(args != null && args.length == 1, "need the server configuration file as argument.")

        // Load properties
        val properties = ConfigUtils.loadConfig(args(0), "UTF-8")

        val configuration = new ServerConfiguration(properties)
        
        val mappingsUrl = new URL(configuration.mappingsUrl)
        
        val localServerUrl = new URI(configuration.localServerUrl)
        
        val serverPassword = configuration.serverPassword
        
        val paths = new Paths(new URL(mappingsUrl, "index.php"), new URL(mappingsUrl, "api.php"), configuration.statisticsDir, configuration.ontologyFile, configuration.mappingsDir)
        
        val languages = configuration.languages
        
        _instance = new Server(serverPassword, languages, paths)
        
        // Configure the HTTP server
        val resources = new PackagesResourceConfig("org.dbpedia.extraction.server.resources", "org.dbpedia.extraction.server.providers")
        
        // redirect URLs like "/foo/../extractionSamples" to "/extractionSamples/" (with a slash at the end)
        val features = resources.getFeatures
        features.put(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, true)
        features.put(ResourceConfig.FEATURE_NORMALIZE_URI, true)
        features.put(ResourceConfig.FEATURE_REDIRECT, true)
        // When trace is on, Jersey includes "X-Trace" headers in the HTTP response.
        // But when it receives a bad URI (e.g. by Apache), Jersey does no tracing. :-( 
        // features.put(ResourceConfig.FEATURE_TRACE, true)

        HttpServerFactory.create(localServerUrl, resources).start()

        logger.info("DBpedia server started in "+prettyMillis(System.currentTimeMillis - millis) + " listening on " + localServerUrl)
    }

    /**
     * Builds template redirects from Wiki statistics as collected by {@link CreateMappingStats}
     * Main purpose is to clean template names from the template namespace so that redirects can be used in Extractors
     * (Extractors use decoded wiki titles)
     * @param redirects
     * @return
     */
    def buildTemplateRedirects(redirects: Map[String, String], language: Language): Redirects = {
      new Redirects(redirects.map { case (from, to) =>
        (WikiTitle.parse(from, language).decoded, WikiTitle.parse(to, language).decoded)
      }.toMap)
    }
}
