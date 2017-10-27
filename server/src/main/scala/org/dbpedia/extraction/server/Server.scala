package org.dbpedia.extraction.server

import java.net.{URI, URL}
import java.util.logging.Logger

import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{PackagesResourceConfig, ResourceConfig}
import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.Server._
import org.dbpedia.extraction.server.stats.MappingStatsManager
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.WikiTitle

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}

class Server(
  private val password : String,
  languages : Seq[Language],
  val paths: Paths,
  mappingTestExtractors: Seq[Class[_ <: Extractor[_]]],
  customTestExtractors: Map[Language, Seq[Class[_ <: Extractor[_]]]])
{
    val managers: SortedMap[Language, MappingStatsManager] = {
      val tuples = languages.map(lang => lang -> new MappingStatsManager(paths.statsDir, lang))
      SortedMap(tuples: _*)
    }

    val redirects: Map[Language, Redirects] = {
      managers.map(manager => (manager._1, buildTemplateRedirects(manager._2.wikiStats.redirects, manager._1)))
    }
        
    val extractor: ExtractionManager = new DynamicExtractionManager(managers(_).updateStats(_), languages, paths, redirects, mappingTestExtractors, customTestExtractors)
    
    extractor.updateAll
        
    def adminRights(pass : String) : Boolean = password == pass
}

/**
 * The DBpedia server.
 * FIXME: more flexible configuration.
 */
object Server
{
    val logger: Logger = Logger.getLogger(getClass.getName)

    private var _instance: Server = _

    def instance: Server = _instance

    private var _config: ServerConfiguration = _

    def config: ServerConfiguration = _config
    
    def main(args : Array[String])
    {
        val millis = System.currentTimeMillis
        
        logger.info("DBpedia server starting")
        
        require(args != null && args.length == 1, "need the server configuration file as argument.")

        // Load properties
        _config = new ServerConfiguration(args(0))
        
        val mappingsUrl = new URL(_config.mappingsUrl)
        
        val localServerUrl = URI.create(_config.localServerUrl)
        
        val serverPassword = _config.serverPassword

        val languages = _config.languages

        val paths = new Paths(new URL(mappingsUrl, "index.php"), new URL(mappingsUrl, "api.php"), _config.statisticsDir, _config.ontologyFile, _config.mappingsDir)
        
        _instance = new Server(serverPassword, languages, paths, _config.mappingTestExtractorClasses, _config.customTestExtractorClasses)
        
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
      })
    }

    private val extractionRecorder = new mutable.HashMap[ClassTag[_], mutable.HashMap[Language, ExtractionRecorder[_]]]()
    def getExtractionRecorder[T: ClassTag](lang: Language, dataset : Dataset = null): ExtractionRecorder[T] = {
        extractionRecorder.get(classTag[T]) match{
            case Some(s) => s.get(lang) match {
                case None =>
                    s(lang) = new ExtractionRecorder[T](null, 2000, null, null, if(dataset != null) List(dataset) else List())
                    s(lang).initialize(lang)
                    s(lang).asInstanceOf[ExtractionRecorder[T]]
                case Some(er) => er.asInstanceOf[ExtractionRecorder[T]]
            }
            case None =>
                extractionRecorder(classTag[T]) = new mutable.HashMap[Language, ExtractionRecorder[_]]()
                getExtractionRecorder[T](lang, dataset)
        }
    }
}
