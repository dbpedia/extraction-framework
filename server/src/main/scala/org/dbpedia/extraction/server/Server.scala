package org.dbpedia.extraction.server

import java.io.File
import java.net.{URI,URL}
import java.util.logging.{Level,Logger}
import scala.collection.immutable.SortedMap
import org.dbpedia.extraction.mappings.{LabelExtractor,MappingExtractor}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.Mappings
import org.dbpedia.extraction.server.stats.MappingStatsManager
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{ResourceConfig,PackagesResourceConfig}
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.Namespace

class Server(private val password : String, wikiUri: URL, languages : Seq[Language], files: FileParams) 
{
    /** 
     * The URL where the pages of the Mappings Wiki are located
     */
    val wikiPagesUrl = new URL(wikiUri, "index.php")

    /** 
     * The URL of the MediaWiki API of the Mappings Wiki 
     */
    val wikiApiUrl = new URL(wikiUri, "api.php")
    
    val managers = SortedMap(languages.map(lang => (lang -> new MappingStatsManager(files.statsDir, lang))): _*)(Language.wikiCodeOrdering)
        
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
        
        val files = new FileParams(new File(args(3)), new File(args(4)), new File(args(5)))
        
        // Use all remaining args as language codes or comma or whitespace separated lists of codes
        var langs : Seq[Language] = for(arg <- args.drop(6); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield Language(lang)
        
        // if no languages are given, use all languages for which a mapping namespace is defined
        if (langs.isEmpty) langs = Namespace.mappings.keySet.toSeq
        
        _instance = new Server(password, wikiUri, langs, files)
        
        // Configure the HTTP server
        val resources = new PackagesResourceConfig("org.dbpedia.extraction.server.resources", "org.dbpedia.extraction.server.providers")
        
        // redirect URLs like "/foo/../extractionSamples" to "/extractionSamples/" (with a slash at the end)
        val features = resources.getFeatures
        features.put(ResourceConfig.FEATURE_CANONICALIZE_URI_PATH, true)
        features.put(ResourceConfig.FEATURE_NORMALIZE_URI, true)
        features.put(ResourceConfig.FEATURE_REDIRECT, true)
        features.put(ResourceConfig.FEATURE_TRACE, true)

        HttpServerFactory.create(uri, resources).start()

        logger.info("DBpedia server started in "+prettyMillis(System.currentTimeMillis - millis) + " listening on " + uri)
    }
}
