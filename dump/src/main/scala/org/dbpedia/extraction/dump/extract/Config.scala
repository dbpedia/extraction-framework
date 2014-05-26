package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.mappings.Extractor
import java.util.Properties
import java.io.File
import org.dbpedia.extraction.wikiparser.Namespace
import scala.collection.Map
import org.dbpedia.extraction.util.{ConfigUtils, ExtractorUtils, Language}
import org.dbpedia.extraction.util.ConfigUtils.{getValue,getStrings}


class Config(config: Properties)
{
  // TODO: get rid of all config file parsers, use Spring

  /** Dump directory */
  val dumpDir = getValue(config, "base-dir", true)(new File(_))
  if (! dumpDir.exists) throw error("dir "+dumpDir+" does not exist")

  val requireComplete = config.getProperty("require-download-complete", "false").toBoolean

  // Watch out, this could be a regex
  val source = config.getProperty("source", "pages-articles.xml.bz2")
  val disambiguations = config.getProperty("disambiguations", "page_props.sql.gz")

  val wikiName = config.getProperty("wikiName", "wiki")

  val parser = config.getProperty("parser", "simple")

  /** Local ontology file, downloaded for speed and reproducibility */
  val ontologyFile = getValue(config, "ontology", false)(new File(_))

  /** Local mappings files, downloaded for speed and reproducibility */
  val mappingsDir = getValue(config, "mappings", false)(new File(_))

  val formats = parseFormats(config, "uri-policy", "format")

  val extractorClasses = loadExtractorClasses()

  val namespaces = loadNamespaces()

  private def loadNamespaces(): Set[Namespace] = {
    val names = getStrings(config, "namespaces", ',', false)
    if (names.isEmpty) Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template)
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

}
