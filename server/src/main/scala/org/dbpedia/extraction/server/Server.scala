package org.dbpedia.extraction.server

import java.net.{URI, URL}
import java.util.logging.Logger

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.sun.jersey.api.container.httpserver.HttpServerFactory
import com.sun.jersey.api.core.{PackagesResourceConfig, ResourceConfig}
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.Server._
import org.dbpedia.extraction.server.stats.MappingStatsManager
import org.dbpedia.extraction.util.{ExtractionRecorder, Language}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.destinations.Destination

import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.{ClassTag, classTag}
import scala.language.existentials

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
    managers.map(manager => (manager._1, Server.buildTemplateRedirects(manager._2.wikiStats.redirects, manager._1)))
    }

  val extractor: ExtractionManager = new DynamicExtractionManager(managers(_).updateStats(_), languages, paths, redirects, mappingTestExtractors, customTestExtractors)

  extractor.updateAll

  // Cache key for single extractor managers
  private case class ExtractorCacheKey(language: Language, extractorClass: Class[_ <: Extractor[_]])

  // Guava LoadingCache for single extractor managers with proper cache loader
  private val singleExtractorCache: LoadingCache[ExtractorCacheKey, ExtractionManager] = {
    CacheBuilder.newBuilder()
      .maximumSize(200)
      //.expireAfterAccess(10, TimeUnit.MINUTES)
      .build(new CacheLoader[ExtractorCacheKey, ExtractionManager]() {
        override def load(key: ExtractorCacheKey): ExtractionManager = {
          val manager = new DynamicExtractionManager(
            managers(_).updateStats(_),
            Seq(key.language),
            paths,
            redirects,
            if (mappingTestExtractors.contains(key.extractorClass)) Seq(key.extractorClass) else Seq.empty,
            if (customTestExtractors.getOrElse(key.language, Seq.empty).contains(key.extractorClass))
              Map(key.language -> Seq(key.extractorClass)) else Map.empty
          )

          try {
            Server.logger.info(s"Initializing extraction manager for ${key.extractorClass.getSimpleName} in ${key.language.wikiCode}")
            manager.updateAll
            Server.logger.info(s"Successfully initialized extraction manager for ${key.extractorClass.getSimpleName}")
            manager
          } catch {
            case e: Exception =>
              Server.logger.severe(s"Failed to initialize extraction manager for ${key.extractorClass.getSimpleName} in ${key.language.wikiCode}: ${e.getMessage}")
              e.printStackTrace()
              throw e
          }
        }
      })
  }


  def adminRights(pass: String): Boolean = password == pass

  /**
   * Enhanced extract using a specific extractor with Guava caching optimization
   */
  def extractWithSpecificExtractor(source: Source, destination: Destination, language: Language, extractorName: String): Unit = {
    // Use ServerConfiguration methods for validation and extractor lookup
    val config = Server.config
    val availableExtractors = config.getAvailableExtractors(language)

    // Find the extractor class
    val extractorClass = findExtractorClass(language, extractorName) match {
      case Some(clazz) =>
        clazz
      case None =>
        throw new IllegalArgumentException(s"Extractor '$extractorName' not found for language '${language.wikiCode}'. Available extractors: ${availableExtractors.mkString(", ")}")
    }

    // Get from Guava cache (will load if not present)
    val cacheKey = ExtractorCacheKey(language, extractorClass)
    val singleExtractorManager = singleExtractorCache.get(cacheKey)

    // Run extraction with the cached manager
    singleExtractorManager.extract(source, destination, language, true)
  }

  /**
   * Find extractor class with flexible matching
   */
  private def findExtractorClass(language: Language, extractorName: String): Option[Class[_ <: Extractor[_]]] = {
    val customExtractorsForLang = customTestExtractors.getOrElse(language, Seq.empty)
    val allExtractors = mappingTestExtractors ++ customExtractorsForLang

    allExtractors.find { extractorClass =>
      val className = extractorClass.getSimpleName
      className == extractorName ||
        className == (extractorName + "Extractor") ||
        className.endsWith(extractorName) ||
        extractorName.endsWith(className) ||
        className.replace("Extractor", "") == extractorName.replace("Extractor", "")
    }
  }

  /**
   * Get available extractor names for a specific language - delegates to ServerConfiguration
   */
  def getAvailableExtractorNames(language: Language): Seq[String] = {
    Server.config.getAvailableExtractors(language)
  }

  /**
   * Check if a specific extractor is available for a language - delegates to ServerConfiguration
   */
  def isExtractorAvailable(language: Language, extractorName: String): Boolean = {
    Server.config.isExtractorAvailable(language, extractorName)
  }

  /**
   * Get cache statistics for monitoring
   */
  def getCacheStats: String = {
    val stats = singleExtractorCache.stats()
    s"Cache stats - Size: ${singleExtractorCache.size()}, Hit Rate: ${stats.hitRate()}, Miss Count: ${stats.missCount()}, Load Count: ${stats.loadCount()}"
  }

  /**
   * Clear the extractor cache if needed
   */
  def clearCache(): Unit = {
    singleExtractorCache.invalidateAll()
  }
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

      /**
     * Returns the Server instance, throwing an exception if not initialized.
     * This centralizes the null checking logic used throughout the application.
     */
    def getInstance(): Server = {
      if (_instance == null) {
        throw new IllegalStateException("Server instance is not initialized")
      }
      _instance
    }

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

      // Initialize all extractors and statistics
      logger.info("Initializing extractors and statistics...")
      _instance.extractor.updateAll
      logger.info("Extractors and statistics initialized")

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
     * Builds template redirects from Wiki statistics as collected by {todo link CreateMappingStats}
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
    def getExtractionRecorder[T: ClassTag](lang: Language, dataset : Dataset = null): org.dbpedia.extraction.util.ExtractionRecorder[T] = {
        extractionRecorder.get(classTag[T]) match{
            case Some(s) => s.get(lang) match {
                case None =>
                    s(lang) = new ExtractionRecorder[T](null, 2000, null, null, if(dataset != null) ListBuffer(dataset) else ListBuffer())
                    s(lang).initialize(lang)
                    s(lang).asInstanceOf[ExtractionRecorder[T]]
                case Some(er) =>
                    if(dataset != null) if(!er.datasets.contains(dataset)) er.datasets += dataset
                    er.asInstanceOf[ExtractionRecorder[T]]
            }
            case None =>
                extractionRecorder(classTag[T]) = new mutable.HashMap[Language, ExtractionRecorder[_]]()
                getExtractionRecorder[T](lang, dataset)
        }
    }
}
