package org.dbpedia.extraction.util

import java.util.logging.{Level, Logger}
import java.util.{MissingResourceException, Locale}

import org.dbpedia.extraction.ontology.{DBpediaNamespace, RdfNamespace}

import scala.collection.mutable.HashMap
import scala.io.{Codec, Source}

/**
 * Represents a MediaWiki instance and the language used on it. Initially, this class was
 * only used for xx.wikipedia.org instances, but now we also use it for mappings.dbpedia.org
 * and www.wikidata.org. For each language, there is only one instance of this class.
 * TODO: rename this class to WikiCode or so, distinguish between enwiki / enwiktionary etc.
 *
 * @param wikiCode "en", "de", "mappings", "wikidata", ...
 * @param isoCode "en", "de", ...
 * @param dbpediaDomain Specific DBpedia domain for this language, e.g. "en.dbpedia.org".
 * May be null, e.g. for mappings.
 * @param dbpediaUri Specific DBpedia base URI for this language, e.g. "http://en.dbpedia.org".
 * May be null, e.g. for mappings.
 * @param resourceUri Specific resource namespace for this language, e.g. "http://en.dbpedia.org/resource/"
 * or "http://www.wikidata.org/entity/". May be null, e.g. for mappings. The value is not a string.
 * Use resourceUri.append("Xy"), not string concatenation. 
 * @param propertyUri Specific property namespace for this language, e.g. "http://en.dbpedia.org/property/"
 * or "http://www.wikidata.org/entity/". May be null, e.g. for mappings. The value is not a string.
 * Use propertyUri.append("xy"), not string concatenation. 
 * @param baseUri URI prefix for this wiki, e.g. "http://be-x-old.wikipedia.org",
 * "http://commons.wikimedia.org", "http://mappings.dbpedia.org".
 * @param apiUri API URI for this wiki, e.g. "https://be-x-old.wikipedia.org/w/api.php",
 * "http://commons.wikimedia.org/w/api.php", "https://mappings.dbpedia.org/api.php".
 */
class Language private(
  val wikiCode: String,
  val name: String,
  val isoCode: String,
  val iso639_3: String,
  val dbpediaDomain: String,
  val dbpediaUri: String,
  val resourceUri: RdfNamespace,
  val propertyUri: RdfNamespace,
  val baseUri: String,
  val apiUri: String
)
{
    val locale = new Locale(isoCode)

    
    /** 
     * Wikipedia dump files use this prefix (with underscores), e.g. be_x_old, but
     * Wikipedia domains use the wikiCode (with dashes), e.g. http://be-x-old.wikipedia.org
     */
    val filePrefix = wikiCode.replace('-', '_')
    /**
     */
    override def toString = "wiki="+wikiCode+",locale="+locale.getLanguage
    
    // no need to override equals() and hashCode() - there is only one object for each value, so equality means identity. 
}

object Language extends (String => Language)
{
  implicit val wikiCodeOrdering = Ordering.by[Language, String](_.wikiCode)

  val logger = Logger.getLogger(Language.getClass.getName)

  val wikipediaLanguageUrl = "https://noc.wikimedia.org/conf/langlist"
  
  val map: Map[String, Language] = locally {
    
    def language(code : String, name: String, iso_1: String, iso_3: String): Language = {
      new Language(
        code,
        name,
        iso_1,
        iso_3,
        code+".dbpedia.org",
        "http://"+code+".dbpedia.org",
        new DBpediaNamespace("http://"+code+".dbpedia.org/resource/"),
        new DBpediaNamespace("http://"+code+".dbpedia.org/property/"),
        "http://"+code+".wikipedia.org",
        "https://"+code+".wikipedia.org/w/api.php"
      )
    }

    val languages = new HashMap[String,Language]
    val source = Source.fromURL(wikipediaLanguageUrl)(Codec.UTF8)
    val wikiLanguageCodes = try source.getLines.toList finally source.close

    val specialLangs: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("addonlangs.json"))

    for (lang <- specialLangs.keys()) {
      {
        val properties = specialLangs.getMap(lang)
        properties.get("dbpediaDomain") match{
          case Some(dom) => languages(lang) = new Language(
            properties.get("wikiCode").get.asText,
            properties.get("name").get.asText,
            properties.get("isoCode").get.asText,
            properties.get("iso639_3").get.asText,
            dom.asText,
            properties.get("dbpediaUri").get.asText(),
            new DBpediaNamespace(properties.get("resourceUri").get.asText),
            new DBpediaNamespace(properties.get("propertyUri").get.asText),
            properties.get("baseUri").get.asText,
            properties.get("apiUri").get.asText
          )
          case None => languages(lang) = language(
            properties.get("wikiCode").get.asText,
            properties.get("name").get.asText,
            properties.get("isoCode").get.asText,
            properties.get("iso639_3").get.asText)
        }
      }
    }

    for (langEntry <- wikiLanguageCodes)
    {
      val loc = new Locale(langEntry)
      try {
        languages(langEntry) = language(langEntry, loc.getDisplayName, loc.getLanguage, loc.getISO3Language)
      }
      catch{
        case mre : MissingResourceException =>
          if(!languages.keySet.contains(langEntry))
            logger.log(Level.WARNING, "Language not found: " + langEntry + ". To extract this language, please edit the addonLanguage.json in core.")
      }
    }

    languages.toMap // toMap makes immutable
  }
  
  /**
   * English Wikipedia
   */
  val English = map("en")
  
  /**
   * DBpedia mappings wiki
   */
  val Mappings = map("mappings")
  
  /**
   * Wikimedia commons
   */
  val Commons = map("commons")

  /**
   * Wikimedia Wikidata
   */
  val Wikidata = map("wikidata")

  /**
    * Wikimedia Wikidata
    */
  val Core = map("core")

  /**
   * Gets a language object for a Wikipedia language code.
   * Throws IllegalArgumentException if language code is unknown.
   */
  def apply(code: String) : Language = map.getOrElse(code, throw new IllegalArgumentException("unknown language code "+code))
  
  /**
   * Gets a language object for a Wikipedia language code, or None if given code is unknown.
   */
  def get(code: String) : Option[Language] = map.get(code)
  
  /**
   * Gets a language object for a Wikipedia language code, or the default if the given code is unknown.
   */
  def getOrElse(code: String, default: => Language) : Language = map.getOrElse(code, default)

}
