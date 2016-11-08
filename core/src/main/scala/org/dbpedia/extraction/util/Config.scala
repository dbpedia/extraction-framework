package org.dbpedia.extraction.util

import java.io.File
import java.net.URL
import java.util.Properties

import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.ConfigUtils.{getStrings, getValue}
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.Map


class Config(config: Properties)
{
  // TODO: get rid of all config file parsers, use Spring

  /**
   * Dump directory
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val dumpDir = getValue(config, "base-dir", true){
    x =>
      val dir = new File(x)
      if (! dir.exists) throw error("dir "+dir+" does not exist")
      dir
  }

  val dbPediaVersion = Option(config.getProperty("dbpedia-version"))

  val requireComplete = config.getProperty("require-download-complete", "false").toBoolean

  val logDir = Option(config.getProperty("log-dir"))

  val retryFailedPages = config.getProperty("retry-failed-pages", "false").toBoolean

  // Watch out, this could be a regex
  val source = config.getProperty("source", "pages-articles.xml.bz2")
  val disambiguations = config.getProperty("disambiguations", "page_props.sql.gz")

  val wikiName = config.getProperty("wikiName", "wiki")

  val parser = config.getProperty("parser", "simple")

  /**
   * Local ontology file, downloaded for speed and reproducibility
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val ontologyFile = getValue(config, "ontology", false)(new File(_))

  /**
   * Local mappings files, downloaded for speed and reproducibility
   * Note: This is lazy to defer initialization until actually called (eg. this class is not used
   * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
   * and overrides this val to null because it is not needed)
   */
  lazy val mappingsDir = getValue(config, "mappings", false)(new File(_))

  val formats = parseFormats(config, "uri-policy", "format")

  val extractorClasses = loadExtractorClasses()

  val namespaces = loadNamespaces()

  private def loadNamespaces(): Set[Namespace] = {
    val names = getStrings(config, "namespaces", ',', false)
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
    val languages = ConfigUtils.parseLanguages(dumpDir,getStrings(config, "languages", ',', false))

    ExtractorUtils.loadExtractorsMapFromConfig(languages, config)
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }

  case class MediaWikiConnection(apiUrl: URL, maxRetries: Int, connectMs: Int, readMs: Int, sleepFactor: Int)
  val mediawikiConnection = MediaWikiConnection(
    apiUrl=new URL(config.getProperty("mwc-apiUrl", "")),
    maxRetries = config.getProperty("mwc-maxRetries", "4").toInt,
    connectMs = config.getProperty("mwc-connectMs", "2000").toInt,
    readMs = config.getProperty("mwc-readMs", "5000").toInt,
    sleepFactor = config.getProperty("mwc-sleepFactor", "1000").toInt
  )

  case class AbstractParameters(abstractQuery: String, abstractTags: String, shortAbstractsProperty: String, longAbstractsProperty: String, shortAbstractMinLength: Int)
  val abstractParameters = AbstractParameters(
    abstractQuery=config.getProperty("abstract-query", ""),
    abstractTags = config.getProperty("abstract-tags", "query,pages,page,extract"),
    shortAbstractsProperty = config.getProperty("short-abstracts-property", "rdfs:comment"),
    longAbstractsProperty = config.getProperty("long-abstracts-property", "abstract"),
    shortAbstractMinLength = config.getProperty("short-abstract-min-length", "200").toInt
  )

  case class NifParameters(nifQuery: String, nifTags: String, isTestRun: Boolean, writeAnchor: Boolean, writeLinkAnchor: Boolean)
  val nifParameters = NifParameters(
    nifQuery=config.getProperty("nif-query", ""),
    nifTags = config.getProperty("nif-tags", "parse,text"),
    isTestRun = config.getProperty("nif-isTestRun", "false").toBoolean,
    writeAnchor = config.getProperty("nif-write-anchor", "false").toBoolean,
    writeLinkAnchor = config.getProperty("nif-write-link-anchor", "true").toBoolean
  )
}
