package org.dbpedia.extraction.util

import java.io.File
import java.net.URL

import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.Map
import scala.util.{Failure, Success}


class Config(configPath: String)
{
  /**
    * load two config files:
    * 1. the universal config containing properties universal for a release
    * 2. the extraction job specific config provided by the user
    */
  val config = ConfigUtils.loadConfig(configPath)

  def checkOverride(key: String) = if(config.containsKey(key))
    config
  else
    universalConfig

  /**
    * get all universal properties, check if there is an override in the provided config file
    */

  /**
   * Dump directory
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val dumpDir = getValue(checkOverride("base-dir"), "base-dir", true){
    x =>
      val dir = new File(x)
      if (! dir.exists) throw error("dir "+dir+" does not exist")
      dir
  }

  lazy val parallelProcesses = checkOverride("parallel-processes").getProperty("parallel-processes", "4").toInt

  lazy val dbPediaVersion = parseVersionString(getString(checkOverride("dbpedia-version"), "dbpedia-version")) match{
    case Success(s) => s
    case Failure(e) => throw new IllegalArgumentException("dbpedia-version option in universal.properties was not defined or in a wrong format", e)
  }

  lazy val logDir = Option(checkOverride("log-dir").getProperty("log-dir"))

  // TODO Watch out, this could be a regex
  lazy val source = checkOverride("source").getProperty("source", "pages-articles.xml.bz2")

  lazy val wikiName = checkOverride("wiki-name").getProperty("wiki-name", "wiki")

  /**
    * Local ontology file, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val ontologyFile = getValue(checkOverride("ontology"), "ontology", false)(new File(_))

  /**
    * Local mappings files, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val mappingsDir = getValue(checkOverride("mappings"), "mappings", false)(new File(_))

  lazy val policies = parsePolicies(checkOverride("uri-policy"), "uri-policy")

  lazy val formats = parseFormats(checkOverride("format"), "format", policies)

  lazy val disambiguations = checkOverride("disambiguations").getProperty("disambiguations", "page_props.sql.gz")

  /**
    *
    *
    * all non universal properties...
    */

  lazy val languages = parseLanguages(dumpDir, getStrings(config, "languages", ","))

  lazy val requireComplete = config.getProperty("require-download-complete", "false").toBoolean

  lazy val retryFailedPages = config.getProperty("retry-failed-pages", "false").toBoolean

  lazy val extractorClasses = loadExtractorClasses()

  lazy val namespaces = loadNamespaces()

  private def loadNamespaces(): Set[Namespace] = {
    val names = getStrings(config, "namespaces", ",")
    if (names.isEmpty) Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template, Namespace.WikidataProperty)
    // Special case for namespace "Main" - its Wikipedia name is the empty string ""
    else names.map(name => if (name.toLowerCase(Language.English.locale) == "main") Namespace.Main else Namespace(Language.English, name)).toSet
  }

  /**
   * Loads the extractors classes from the configuration.
   * Loads only the languages defined in the languages property
   *
   * @return A Map which contains the extractor classes for each language
   */
  private def loadExtractorClasses() : Map[Language, Seq[Class[_ <: Extractor[_]]]] =
  {
    ExtractorUtils.loadExtractorsMapFromConfig(languages, config)
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }

  case class MediaWikiConnection(apiUrl: String, maxRetries: Int, connectMs: Int, readMs: Int, sleepFactor: Int)
  lazy val mediawikiConnection = MediaWikiConnection(
    apiUrl=config.getProperty("mwc-apiUrl", ""),
    maxRetries = config.getProperty("mwc-maxRetries", "4").toInt,
    connectMs = config.getProperty("mwc-connectMs", "2000").toInt,
    readMs = config.getProperty("mwc-readMs", "5000").toInt,
    sleepFactor = config.getProperty("mwc-sleepFactor", "1000").toInt
  )

  case class AbstractParameters(abstractQuery: String, abstractTags: String, shortAbstractsProperty: String, longAbstractsProperty: String, shortAbstractMinLength: Int)
  lazy val abstractParameters = AbstractParameters(
    abstractQuery=config.getProperty("abstract-query", ""),
    abstractTags = config.getProperty("abstract-tags", "query,pages,page,extract"),
    shortAbstractsProperty = config.getProperty("short-abstracts-property", "rdfs:comment"),
    longAbstractsProperty = config.getProperty("long-abstracts-property", "abstract"),
    shortAbstractMinLength = config.getProperty("short-abstract-min-length", "200").toInt
  )

  case class NifParameters(
    nifQuery: String,
    nifTags: String,
    isTestRun: Boolean,
    writeAnchor: Boolean,
    writeLinkAnchor: Boolean,
    cssSelectorMap: URL
  )
  lazy val nifParameters = NifParameters(
    nifQuery=config.getProperty("nif-query", ""),
    nifTags = config.getProperty("nif-tags", "parse,text"),
    isTestRun = config.getProperty("nif-isTestRun", "false").toBoolean,
    writeAnchor = config.getProperty("nif-write-anchor", "false").toBoolean,
    writeLinkAnchor = config.getProperty("nif-write-link-anchor", "true").toBoolean,
    cssSelectorMap = this.getClass.getClassLoader.getResource("nifextractionconfig.json")   //static config file in core/src/main/resources
  )
}
