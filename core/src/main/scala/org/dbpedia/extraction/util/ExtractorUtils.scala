package org.dbpedia.extraction.util

import org.dbpedia.extraction.mappings.Extractor
import scala.collection.mutable.HashMap
import java.util.Properties
import scala.collection.JavaConversions.asScalaSet
import scala.collection.Map
import scala.collection.immutable.SortedMap
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.ConfigUtils.{getStrings}
import org.dbpedia.extraction.util.RichString.wrapString

/**
 * User: Dimitris Kontokostas
 * Various utils for loading Extractors
 * I don't like this so much but it's the only way to reuse extraction configuration code on multiple modules (dump / server)
 * Created: 5/19/14 11:06 AM
 */
object ExtractorUtils {

  /*
   * Get an Extractor class from it's name.
   * if name starts with . use "org.dbpedia.extraction.mappings" as prefix
   * */
  def loadExtractorClass(name: String): Class[_ <: Extractor[_]] = {
    val className = if (name.startsWith(".")) classOf[Extractor[_]].getPackage.getName+name else name
    // TODO: class loader of Extractor.class is probably wrong for some users.
    classOf[Extractor[_]].getClassLoader.loadClass(className).asSubclass(classOf[Extractor[_]])
  }

  def loadExtractorClassSeq(names: Seq[String]) : Seq[Class[_ <: Extractor[_]]] = {
    names.toList.map(ExtractorUtils.loadExtractorClass)
  }

  /** creates a map of languages and extractors from a Properties file
    * examples are as follows
    * # default for all languages
    * extractors=.ArticleCategoriesExtractor,.ArticleTemplatesExtractor,
    *
    * # custom per language
    * extractors.bg=.MappingExtractor
    *
    * Note: If a language xx is not defined but extractor.xx exists
    * extractor.xx extractors will be skipped
    * */
  def loadExtractorsMapFromConfig(languages: Seq[Language], config: Properties): Map[Language, Seq[Class[_ <: Extractor[_]]]] = {

    val stdExtractors = loadExtractorClassSeq(getStrings(config, "extractors", ',', false))

    val classes = new HashMap[Language, Seq[Class[_ <: Extractor[_]]]]()

    for(language <- languages) {
      classes(language) = stdExtractors
    }

    for (key <- config.stringPropertyNames) {
      if (key.startsWith("extractors.")) {
        val language = Language(key.substring("extractors.".length()))
        if (languages.contains(language)) { // load language only if it declared explicitly
          classes(language) = (stdExtractors ++ getStrings(config, key, ',', true).map(loadExtractorClass)).distinct
        }
      }
    }

    SortedMap(classes.toSeq: _*)
  }
}
