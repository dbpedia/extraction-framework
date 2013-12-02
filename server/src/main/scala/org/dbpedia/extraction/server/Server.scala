package org.dbpedia.extraction.server

import java.io.File
import java.net.{URI,URL}
import java.util.logging.{Level,Logger}
import scala.collection.immutable.SortedMap
import org.dbpedia.extraction.mappings.{Redirects, LabelExtractor, MappingExtractor, Mappings}
import org.dbpedia.extraction.util.Language
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
        
        require(args != null && args.length >= 6, "need at least six args: server URL, mappings wiki base URL, password for template ignore list, base dir for statistics, ontology file, mappings dir. Additional args are wiki codes for languages.")
        
        val wikiUri = new URL(args(0))
        
        val uri = new URI(args(1))
        
        val password = args(2)
        
        val paths = new Paths(new URL(wikiUri, "index.php"), new URL(wikiUri, "api.php"), new File(args(3)), new File(args(4)), new File(args(5)))
        
        // Use all remaining args as language codes or comma or whitespace separated lists of codes
        var langs : Seq[Language] = for(arg <- args.drop(6); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield Language(lang)
        
        // if no languages are given, use all languages for which a mapping namespace is defined
        if (langs.isEmpty) langs = Namespace.mappings.keySet.toSeq
        
        _instance = new Server(password, langs, paths)
        
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

        HttpServerFactory.create(uri, resources).start()

        logger.info("DBpedia server started in "+prettyMillis(System.currentTimeMillis - millis) + " listening on " + uri)
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
