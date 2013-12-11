package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.mappings.{Extractor, PageNodeExtractor}
import scala.collection.mutable.HashMap
import java.util.Properties
import java.io.File
import org.dbpedia.extraction.wikiparser.Namespace
import scala.collection.JavaConversions.asScalaSet
import scala.collection.Map
import scala.collection.immutable.{SortedSet,SortedMap}
import org.dbpedia.extraction.util.{Language,WikiInfo}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.ConfigUtils.{LanguageRegex,RangeRegex,toRange,getValue,getStrings}
import org.dbpedia.extraction.util.RichString.wrapString
import scala.io.Codec

private class Config(config: Properties)
{
  // TODO: get rid of all config file parsers, use Spring

  /** Dump directory */
  val dumpDir = getValue(config, "base-dir", true)(new File(_))
  if (! dumpDir.exists) throw error("dir "+dumpDir+" does not exist")

  val requireComplete = config.getProperty("require-download-complete", "false").toBoolean

  // Watch out, this could be a regex
  val source = config.getProperty("source", "pages-articles.xml")
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
   *
   * @return A Map which contains the extractor classes for each language
   */
  private def loadExtractorClasses() : Map[Language, List[Class[_ <: Extractor[_]]]] =
  {
    val languages = loadLanguages()

    val stdExtractors = getStrings(config, "extractors", ',', false).toList.map(loadExtractorClass)

    //Create extractor map
    // type of Extractor would be 'any' because they are different types of extractors and not categorized yet
    val classes = new HashMap[Language, List[Class[_ <: Extractor[_]]]]()

    /*
    TODO: maybe we should check in the first loop if property "extractors."+language.wikiCode
    exists and if it does, add its specific extractors. Better: refactor the whole config mess.
    Currently, the "languages" property just defines for which languages the default extractors
    should be loaded. It does not define which languages should be processed in general,
    all the "extractors.xx" properties are independent from the "languages" property.
    It should be possible to say: run extractors A,B,C for languages xx,yy,zz. That
    would make the configuration much simpler, less repetitive and more flexible.
    */
    for(language <- languages) {
      classes(language) = stdExtractors
    }

    for (key <- config.stringPropertyNames) {
      if (key.startsWith("extractors.")) {
        val language = Language(key.substring("extractors.".length()))
        classes(language) = stdExtractors ++ getStrings(config, key, ',', true).map(loadExtractorClass)
      }
    }

    SortedMap(classes.toSeq: _*)
  }

  private def loadLanguages(): Set[Language] = {

    /** Languages */
    // TODO: add special parameters, similar to download:
    // extract=10000-:InfoboxExtractor,PageIdExtractor means all languages with at least 10000 articles
    // extract=mapped:MappingExtractor means all languages with a mapping namespace

    val keys = getStrings(config, "languages", ',', false)

    var languages = Set[Language]()

    var ranges = Set[(Int,Int)]()

    // FIXME: copy & paste in DownloadConfig and ConfigUtils

    for (key <- keys) key match {
      case "@mappings" => languages ++= Namespace.mappings.keySet
      case RangeRegex(from, to) => ranges += toRange(from, to)
      case LanguageRegex(language) => languages += Language(language)
      case other => throw new Exception("Invalid language / range '"+other+"'")
    }

    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(dumpDir, WikiInfo.FileName)

      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      println("parsing "+listFile)
      val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)

      // for all wikis in one of the desired ranges...
      for ((from, to) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add its language
        languages += wiki.language
      }
    }

    SortedSet[Language](languages.toSeq: _*)
  }

  private def loadExtractorClass(name: String): Class[_ <: Extractor[_]] = {
    val className = if (name.startsWith(".")) classOf[Extractor[_]].getPackage.getName+name else name
    // TODO: class loader of Extractor.class is probably wrong for some users.
    classOf[Extractor[_]].getClassLoader.loadClass(className).asSubclass(classOf[Extractor[_]])
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }

}