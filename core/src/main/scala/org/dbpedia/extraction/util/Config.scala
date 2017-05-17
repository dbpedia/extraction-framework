package org.dbpedia.extraction.util

import java.io.File
import java.net.URL
import java.util.Properties
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.Config.{AbstractParameters, MediaWikiConnection, NifParameters, SlackCredentials}
import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.Map
import scala.io.Codec
import scala.util.{Failure, Success, Try}


class Config(val configPath: String)
{
  val logger = Logger.getLogger(getClass.getName)
  /**
    * load two config files:
    * 1. the universal config containing properties universal for a release
    * 2. the extraction job specific config provided by the user
    */

  private val properties: Properties = if(configPath == null) Config.universalProperties else ConfigUtils.loadConfig(configPath)

  private def checkOverride(key: String): Properties = if(properties.containsKey(key))
    properties
  else
    Config.universalProperties

  def getArbitraryStringProperty(key: String): Option[String] = {
    Option(getString(checkOverride(key), key))
  }

  def throwMissingPropertyException(property: String, required: Boolean): Unit ={
    if(required)
      throw new IllegalArgumentException("The following required property is missing from the provided .properties file (or has an invalid format): '" + property + "'")
    else
      logger.log(Level.WARNING, "The following property is missing from the provided .properties file (or has an invalid format): '" + property + "'. It will not factor in.")
  }

  /**
    * get all universal properties, check if there is an override in the provided config file
    */


  // TODO Watch out, this could be a regex
  lazy val source: String = checkOverride("source").getProperty("source", "pages-articles.xml.bz2").trim

  lazy val wikiName: String = checkOverride("wiki-name").getProperty("wiki-name", "wiki").trim

  /**
   * Dump directory
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val dumpDir: File = getValue(checkOverride("base-dir"), "base-dir", required = true){ x => new File(x)}

  lazy val parallelProcesses: Int = checkOverride("parallel-processes").getProperty("parallel-processes", "4").trim.toInt

  lazy val dbPediaVersion: String = parseVersionString(getString(checkOverride("dbpedia-version"), "dbpedia-version").trim) match{
    case Success(s) => s
    case Failure(e) => throw new IllegalArgumentException("dbpedia-version option in universal.properties was not defined or in a wrong format", e)
  }

  lazy val wikidataMappingsFile: File = {
    val name = checkOverride("wikidata-property-mappings-file").getProperty("wikidata-property-mappings-file", "wikidata-property-mappings.json").trim
    new File(dumpDir, name)
  }

  /**
    * The directory where all log files will be stored
    */
  lazy val logDir: Option[File] = Option(getString(checkOverride("log-dir"), "log-dir")) match {
    case Some(x) => Some(new File(x))
    case None => None
  }

  /**
    * If set, extraction summaries are forwarded via the API of Slack, displaying messages on a dedicated channel.
    * The URL of the slack webhook to be used
    * the username under which all messages are posted (has to be registered for this webhook?)
    * Threshold of extracted pages over which a summary of the current extraction is posted
    * Threshold of exceptions over which an exception report is posted
    */
  lazy val slackCredentials = Try{
      SlackCredentials(
        webhook = new URL(getString(checkOverride("slack-webhook"), "slack-webhook").trim),
        username = checkOverride("slack-username").getProperty("slack-username").trim,
        summaryThreshold = checkOverride("slack-summary-threshold").getProperty("slack-summary-threshold").trim.toInt,
        exceptionThreshold = checkOverride("slack-exception-threshold").getProperty("slack-exception-threshold").trim.toInt
      )}

  /**
    * Local ontology file, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val ontologyFile: File = getValue(checkOverride("ontology"), "ontology", required = false)(new File(_))

  /**
    * Local mappings files, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    */
  lazy val mappingsDir: File = getValue(checkOverride("mappings"), "mappings", required = false)(new File(_))

  lazy val policies: Map[String, Array[Policy]] = parsePolicies(checkOverride("uri-policy"), "uri-policy")

  lazy val formats: Map[String, Formatter] = parseFormats(checkOverride("format"), "format", policies)

  lazy val disambiguations: String = checkOverride("disambiguations").getProperty("disambiguations", "page_props.sql.gz")

  /**
    * all non universal properties...
    */
  /**
    * An array of input dataset names (e.g. 'instance-types' or 'mappingbased-literals') (separated by a ',')
    */
  lazy val inputDatasets: Seq[String] = getStrings(checkOverride("input"), "input", ",").distinct   //unique collection of names!!!
  /**
    * A dataset name for the output file generated (e.g. 'instance-types' or 'mappingbased-literals')
    */
  lazy val outputDataset: Option[String] = Option(getString(checkOverride("output"), "output"))
  /**
    * the suffix of the files representing the input dataset (usually a combination of RDF serialization extension and compression used - e.g. .ttl.bz2 when using the TURTLE triples compressed with bzip2)
    */
  lazy val inputSuffix: Option[String] = Option(getString(checkOverride("suffix"), "suffix"))
  /**
    * same as for inputSuffix (for the output dataset)
    */
  lazy val outputSuffix: Option[String] = Option(getString(checkOverride("output-suffix"), "output-suffix"))
  /**
    * instead of a defined output dataset name, one can specify a name extension turncated at the end of the input dataset name (e.g. '-transitive' -> instance-types-transitive)
    */
  lazy val datasetnameExtension: Option[String] = Option(getString(checkOverride("name-extension"), "name-extension"))

  /**
    * An array of languages specified by the exact enumeration of language wiki codes (e.g. en,de,fr...)
    * or article count ranges ('10000-20000' or '10000-' -> all wiki languages having that much articles...)
    * or '@mappings', '@chapters' when only mapping/chapter languages are of concern
    * or '@downloaded' if all downloaded languages are to be processed (containing the download.complete file)
    * or '@abstracts' to only process languages which provide human readable abstracts (thus not 'wikidata' and the like...)
    */
  lazy val languages: Array[Language] = parseLanguages(dumpDir, getStrings(checkOverride("languages"), "languages", ","), wikiName)

  /**
    * before processing a given language, check if the download.complete file is present
    */
  lazy val requireComplete: Boolean = checkOverride("require-download-complete").getProperty("require-download-complete", "false").trim.toBoolean

  /**
    * TODO experimental, ignore for now
    */
  lazy val retryFailedPages: Boolean = checkOverride("retry-failed-pages").getProperty("retry-failed-pages", "false").trim.toBoolean

  /**
    * the extractor classes to be used when extracting the XML dumps
    */
  lazy val extractorClasses: Map[Language, Seq[Class[_ <: Extractor[_]]]] = loadExtractorClasses()

  /**
    * namespaces loaded defined by the languages in use (see languages)
    */
  lazy val namespaces: Set[Namespace] = loadNamespaces()

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
    ExtractorUtils.loadExtractorsMapFromConfig(languages, properties)
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

  private val universalProperties: Properties = loadConfig(this.getClass.getClassLoader.getResource("universal.properties")).asInstanceOf[Properties]
  val universalConfig: Config = new Config(null)

  private val wikisinfoFile = new File(getString(universalProperties , "base-dir", true), WikiInfo.FileName)
  private lazy val wikiinfo = if(wikisinfoFile.exists()) WikiInfo.fromFile(wikisinfoFile, Codec.UTF8) else Seq()
  def wikiInfos = wikiinfo
}
