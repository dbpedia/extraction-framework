package org.dbpedia.extraction.util

import java.io.File
import java.net.URL

import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.Config.{AbstractParameters, MediaWikiConnection, NifParameters, SlackCredentials}
import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.Map
import scala.util.{Failure, Success}


class Config(val configPath: String)
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


  // TODO Watch out, this could be a regex
  lazy val source = checkOverride("source").getProperty("source", "pages-articles.xml.bz2").trim

  lazy val wikiName = checkOverride("wiki-name").getProperty("wiki-name", "wiki").trim

  /**
   * Dump directory
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val dumpDir = getValue(checkOverride("base-dir"), "base-dir", required = true){ x => new File(x)}

  lazy val parallelProcesses = checkOverride("parallel-processes").getProperty("parallel-processes", "4").trim.toInt

  lazy val dbPediaVersion = parseVersionString(getString(checkOverride("dbpedia-version"), "dbpedia-version").trim) match{
    case Success(s) => s
    case Failure(e) => throw new IllegalArgumentException("dbpedia-version option in universal.properties was not defined or in a wrong format", e)
  }

  /**
    * The directory where all log files will be stored
    */
  lazy val logDir = Option(checkOverride("log-dir").getProperty("log-dir").trim)

  /**
    * If set, extraction summaries are forwarded via the API of Slack, displaying messages on a dedicated channel.
    * The URL of the slack webhook to be used
    * the username under which all messages are posted (has to be registered for this webhook?)
    * Threshold of extracted pages over which a summary of the current extraction is posted
    * Threshold of exceptions over which an exception report is posted
    */
  lazy val slackCredentials = if(checkOverride("slack-webhook").getProperty("slack-webhook") == null)
    null
  else
    SlackCredentials(
      webhook = new URL(checkOverride("slack-webhook").getProperty("slack-webhook").trim),
      username = checkOverride("slack-username").getProperty("slack-username").trim,
      summaryThreshold = checkOverride("slack-summary-threshold").getProperty("slack-summary-threshold").trim.toInt,
      exceptionThreshold = checkOverride("slack-exception-threshold").getProperty("slack-exception-threshold").trim.toInt
    )

  /**
    * Local ontology file, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val ontologyFile = getValue(checkOverride("ontology"), "ontology", required = false)(new File(_))

  /**
    * Local mappings files, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val mappingsDir = getValue(checkOverride("mappings"), "mappings", required = false)(new File(_))

  lazy val policies = parsePolicies(checkOverride("uri-policy"), "uri-policy")

  lazy val formats = parseFormats(checkOverride("format"), "format", policies)

  lazy val disambiguations = checkOverride("disambiguations").getProperty("disambiguations", "page_props.sql.gz")

  /**
    * all non universal properties...
    */

  lazy val languages = parseLanguages(dumpDir, getStrings(checkOverride("languages"), "languages", ","), wikiName)

  lazy val requireComplete = checkOverride("require-download-complete").getProperty("require-download-complete", "false").trim.toBoolean

  lazy val retryFailedPages = checkOverride("retry-failed-pages").getProperty("retry-failed-pages", "false").trim.toBoolean

  lazy val extractorClasses = loadExtractorClasses()

  lazy val namespaces = loadNamespaces()

  private def loadNamespaces(): Set[Namespace] = {
    val names = getStrings(checkOverride("namespaces"), "namespaces", ",")
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

  lazy val mediawikiConnection = MediaWikiConnection(
    apiUrl=checkOverride("mwc-apiUrl").getProperty("mwc-apiUrl", "").trim,
    maxRetries = checkOverride("mwc-maxRetries").getProperty("mwc-maxRetries", "4").trim.toInt,
    connectMs = checkOverride("mwc-connectMs").getProperty("mwc-connectMs", "2000").trim.toInt,
    readMs = checkOverride("mwc-readMs").getProperty("mwc-readMs", "5000").trim.toInt,
    sleepFactor = checkOverride("mwc-sleepFactor").getProperty("mwc-sleepFactor", "1000").trim.toInt
  )

  lazy val abstractParameters = AbstractParameters(
    abstractQuery=checkOverride("abstract-query").getProperty("abstract-query", "").trim,
    shortAbstractsProperty = checkOverride("short-abstracts-property").getProperty("short-abstracts-property", "rdfs:comment").trim,
    longAbstractsProperty = checkOverride("long-abstracts-property").getProperty("long-abstracts-property", "abstract").trim,
    shortAbstractMinLength = checkOverride("short-abstract-min-length").getProperty("short-abstract-min-length", "200").trim.toInt,
    abstractTags = checkOverride("abstract-tags").getProperty("abstract-tags", "query,pages,page,extract").trim
  )


  lazy val nifParameters = NifParameters(
    nifQuery=checkOverride("nif-query").getProperty("nif-query", "").trim,
    nifTags = checkOverride("nif-tags").getProperty("nif-tags", "parse,text").trim,
    isTestRun = checkOverride("nif-isTestRun").getProperty("nif-isTestRun", "false").trim.toBoolean,
    writeAnchor = checkOverride("nif-write-anchor").getProperty("nif-write-anchor", "false").trim.toBoolean,
    writeLinkAnchor = checkOverride("nif-write-link-anchor").getProperty("nif-write-link-anchor", "true").trim.toBoolean,
    abstractsOnly = checkOverride("nif-extract-abstract-only").getProperty("nif-extract-abstract-only", "true").trim.toBoolean,
    cssSelectorMap = this.getClass.getClassLoader.getResource("nifextractionconfig.json")   //static config file in core/src/main/resources
  )
}

object Config{
  case class NifParameters(
    nifQuery: String,
    nifTags: String,
    isTestRun: Boolean,
    writeAnchor: Boolean,
    writeLinkAnchor: Boolean,
    abstractsOnly: Boolean,
    cssSelectorMap: URL
  )

  case class MediaWikiConnection(
    apiUrl: String,
    maxRetries: Int,
    connectMs: Int,
    readMs: Int,
    sleepFactor: Int
  )

  case class AbstractParameters(
    abstractQuery: String,
    shortAbstractsProperty: String,
    longAbstractsProperty: String,
    shortAbstractMinLength: Int,
    abstractTags: String
  )

  case class SlackCredentials(
     webhook: URL,
     username: String,
     summaryThreshold: Int,
     exceptionThreshold: Int
   )
}
