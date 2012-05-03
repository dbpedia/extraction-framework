package org.dbpedia.extraction.server

import java.io.File
import java.net.{URI,URL}
import java.util.logging.{Level,Logger}
import scala.collection.immutable.SortedSet
import org.dbpedia.extraction.mappings.{LabelExtractor,MappingExtractor}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.Mappings
import org.dbpedia.extraction.server.stats.MappingStatsManager
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{ResourceConfig,PackagesResourceConfig}
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.Namespace

class Server(private val password : String, val languages : Set[Language], files: FileParams) 
{
    val managers = languages.map(language => (language -> new MappingStatsManager(files.statsDir, language))).toMap
        
    val extractor = new DynamicExtractionManager(managers(_).updateMappings(_), languages, files)
    
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

    // Note: we use /server/ because that's what we use on http://mappings.dbpedia.org/server/
    // It makes handling redirects easier.
    val serverURI = new URI("http://localhost:9998/server/")

    /** 
     * The URL where the pages of the Mappings Wiki are located
     * TODO: make this configurable 
     */
    val wikiPagesUrl = new URL("http://mappings.dbpedia.org/index.php")

    /** 
     * The URL of the MediaWiki API of the Mappings Wiki 
     * TODO: make this configurable 
     */
    val wikiApiUrl = new URL("http://mappings.dbpedia.org/api.php")
    
    private var _instance: Server = null
    
    def instance = _instance
    
    def main(args : Array[String])
    {
        val millis = System.currentTimeMillis
        
        logger.info("DBpedia server starting")
        
        require(args != null && args.length >= 4, "need at least four args: password for template ignore list, base dir for statistics, ontology file, mappings dir. Additional args are wiki codes for languages.")
        val password = args(0)
        
        val files = new FileParams(new File(args(1)), new File(args(2)), new File(args(3)))
        
        // Use all remaining args as language codes or comma or whitespace separated lists of codes
        var langs : Seq[Language] = for(arg <- args.drop(4); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield Language(lang)
        
        // if no languages are given, use all languages for which a mapping namespace is defined
        if (langs.isEmpty) langs = Namespace.mappings.keySet.toSeq
        
        val languages = SortedSet(langs: _*)(Language.wikiCodeOrdering)
        
        _instance = new Server(password, languages, files)
        
        // Configure the HTTP server
        val resources = new PackagesResourceConfig("org.dbpedia.extraction.server.resources", "org.dbpedia.extraction.server.providers")
        
        // redirect URLs like "/foo/../extractionSamples" to "/extractionSamples/" (with a slash at the end)
        val features = resources.getFeatures
        features.put(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, true)
        features.put(ResourceConfig.FEATURE_NORMALIZE_URI, true)
        features.put(ResourceConfig.FEATURE_REDIRECT, true)

        HttpServerFactory.create(serverURI, resources).start()

        logger.info("DBpedia server started in "+prettyMillis(System.currentTimeMillis - millis) + " listening on " + serverURI)
    }
}
