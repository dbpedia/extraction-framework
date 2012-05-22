package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.mappings.Extractor
import scala.collection.immutable.ListMap
import java.util.Properties
import java.io.File
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace

private class Config(config: Properties)
extends ConfigParser(config)
{
  // TODO: rewrite this, similar to download stuff:
  // - Don't use java.util.Properties, allow multiple values for one key
  // - Resolve config file names and load them as well
  // - Use pattern matching to parse arguments
  // - allow multiple config files, given on command line

  /** Dump directory */
  val dumpDir = getFile("dir")
  if (dumpDir == null) throw error("property 'dir' not defined.")
  if (! dumpDir.exists) throw error("dir "+dumpDir+" does not exist")
  
  val requireComplete = config.getProperty("require-download-complete") != null &&
    config.getProperty("require-download-complete").toBoolean

  /** Local ontology file, downloaded for speed and reproducibility */
  val ontologyFile = getFile("ontology")

  /** Local mappings files, downloaded for speed and reproducibility */
  val mappingsDir = getFile("mappings")
  
  val formats = new PolicyParser(config).parseFormats()

  /** Languages */
  // TODO: add special parameters, similar to download:
  // extract=10000-:InfoboxExtractor,PageIdExtractor means all languages with at least 10000 articles
  // extract=mapped:MappingExtractor means all languages with a mapping namespace
  var languages = splitValue("languages", ',').map(Language)
  if (languages.isEmpty) languages = Namespace.mappings.keySet.toList
  languages = languages.sorted(Language.wikiCodeOrdering)

  val extractorClasses = loadExtractorClasses()
  
  private def getFile(key: String): File = {
    val value = config.getProperty(key)
    if (value == null) null else new File(value)
  }
  
  /**
   * Loads the extractors classes from the configuration.
   *
   * @return A Map which contains the extractor classes for each language
   */
  private def loadExtractorClasses() : Map[Language, List[Class[_ <: Extractor]]] =
  {
    //Load extractor classes
    if(config.getProperty("extractors") == null) throw error("Property 'extractors' not defined.")
    val stdExtractors = splitValue("extractors", ',').map(loadExtractorClass)

    //Create extractor map
    var extractors = ListMap[Language, List[Class[_ <: Extractor]]]()
    for(language <- languages) extractors += ((language, stdExtractors))

    //Load language specific extractors
    val LanguageExtractor = """extractors\.(.*)""".r

    for(LanguageExtractor(code) <- config.stringPropertyNames.toArray)
    {
        val language = Language(code)
        if (extractors.contains(language))
        {
            extractors += language -> (stdExtractors ::: splitValue("extractors."+code, ',').map(loadExtractorClass))
        }
    }

    extractors
  }

  private def loadExtractorClass(name: String): Class[_ <: Extractor] = {
    val className = if (! name.contains(".")) classOf[Extractor].getPackage.getName+'.'+name else name
    // TODO: class loader of Extractor.class is probably wrong for some users.
    classOf[Extractor].getClassLoader.loadClass(className).asSubclass(classOf[Extractor])
  }
  
}

