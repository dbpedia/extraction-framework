package org.dbpedia.extraction.util

import java.io.File
import java.util.logging.{Level, Logger}
import java.util.{Locale, MissingResourceException}

import org.dbpedia.extraction.config.Config
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
  val iso639_2: String,
  val iso639_3: String,
  val dbpediaDomain: String,
  val dbpediaUri: String,
  val resourceUri: RdfNamespace,
  val propertyUri: RdfNamespace,
  val baseUri: String,
  val apiUri: String,
  val pages: Int
)
{
    val locale = new Locale(isoCode)
    
    /** 
     * Wikipedia dump files use this prefix (with underscores), e.g. be_x_old, but
     * Wikipedia domains use the wikiCode (with dashes), e.g. http://be-x-old.wikipedia.org
     */
    val filePrefix: String = wikiCode.replace('-', '_')
    /**
     */
    override def toString: String = "wiki="+wikiCode+",locale="+locale.getLanguage
    
    // no need to override equals() and hashCode() - there is only one object for each value, so equality means identity. 
}

object Language extends (String => Language)
{
  implicit val wikiCodeOrdering: Ordering[Language] = Ordering.by[Language, String](_.name).reverse

  val logger: Logger = Logger.getLogger(Language.getClass.getName)

  val wikipediaLanguageUrl = "https://noc.wikimedia.org/conf/langlist"
  
  val map: Map[String, Language] = locally {
    def language(code : String, name: String, iso_1: String, iso_2: String, iso_3: String): Language = {
      val c = code.trim.toLowerCase
      val baseDomain = if(c.trim.toLowerCase == "en") "dbpedia.org" else c + ".dbpedia.org"
      new Language(
        c,
        name.trim,
        iso_1.trim,
        iso_2,
        iso_3.trim,
        baseDomain,
        "http://" + baseDomain,
        new DBpediaNamespace("http://" + baseDomain + "/resource/"),
        new DBpediaNamespace("http://" + baseDomain + "/property/"),
        "http://"+c+".wikipedia.org",
        "https://"+c+".wikipedia.org/w/api.php",
        Config.wikiInfos.filter(x => x.wikicode == code) match{
          case e if e.nonEmpty => e.head.pages
          case _ => 0
        }
      )
    }

    val languages = new HashMap[String,Language]
    val source = Source.fromURL(wikipediaLanguageUrl)(Codec.UTF8)
    val wikiLanguageCodes = try source.getLines.toList finally source.close


    //extract additional iso codes from world fact dataset
    var iso1Map = Map[String, String]()
    var iso2Map = Map[String, String]()
    val wfRegex = """^\s*<http://worldfacts.dbpedia.org/languages/([^>]+)>\s*<http://worldfacts.dbpedia.org/ontology/iso([^>]+)>\s*"(\w+)"\s*.\s*$""".r
    IOUtils.readLines(this.getClass.getClassLoader.getResourceAsStream("worldfacts_dbpedia_data.ttl.bz2"), Codec.UTF8.charSet) { line =>
      if (line != null) {
        wfRegex.findFirstMatchIn(line) match {
          case Some(m) =>
            m.group(2) match {
              case "639-3" => assert(m.group(3) == m.group(1))
              case "639-2B" => iso2Map += m.group(1) -> m.group(3)
              case "639-1" => iso1Map += m.group(1) -> m.group(3)
              case _ =>
            }
          case scala.None =>
        }
      }
    }

    val specialLangs: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("addonlangs.json"))
    for (lang <- specialLangs.keys()) {
      {
        val properties = specialLangs.getMap(lang)
        properties.get("dbpediaDomain") match{
          case Some(dom) => languages(lang) = new Language(
            properties("wikiCode").asText,
            properties("name").asText,
            properties("isoCode").asText,
            iso2Map.get(properties("iso639_3").asText) match{
              case Some(s) => s
              case scala.None => null
            },
            properties("iso639_3").asText,
            dom.asText,
            properties("dbpediaUri").asText(),
            new DBpediaNamespace(properties("resourceUri").asText),
            new DBpediaNamespace(properties("propertyUri").asText),
            properties("baseUri").asText,
            properties("apiUri").asText,
            properties("pages").asInt
          )
          case scala.None => languages(lang) = language(
            properties("wikiCode").asText,
            properties("name").asText,
            properties("isoCode").asText,
            iso2Map.get(properties("iso639_3").asText) match{
              case Some(s) => s
              case scala.None => null
            },
            properties("iso639_3").asText)
        }
      }
    }

    for (langEntry <- wikiLanguageCodes)
    {
      val loc = new Locale(langEntry)
      try {
        val iso2 = iso2Map.get(loc.getISO3Language) match{
          case Some(s) => s
          case scala.None => null
        }
        languages(langEntry) = language(langEntry, loc.getDisplayName, loc.getLanguage, iso2, loc.getISO3Language)
      }
      catch{
        case mre : MissingResourceException =>
          if(!languages.keySet.contains(langEntry))
            logger.log(Level.WARNING, "Language not found: " + langEntry + ". To extract this language, please edit the addonLanguage.json in core.")
      }
    }
    iso1Map = null
    iso2Map = null
    languages.toMap // toMap makes immutable
  }
  
  /**
   * English Wikipedia
   */
  val English: Language = map("en")
  
  /**
   * DBpedia mappings wiki
   */
  val Mappings: Language = map("mappings")
  
  /**
   * Wikimedia commons
   */
  val Commons: Language = map("commons")

  /**
   * Wikimedia Wikidata
   */
  val Wikidata: Language = map("wikidata")

  /**
    * The Core Directory as a quasi language
    */
  val Core: Language = map("core")

  /**
    * Alibi Language
    */
  val None: Language = map("none")

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
    * Will try every known iso639 code to resolve to a Language
    * Only use this function to retrieve by iso codes (use get for wiki codes)
    * test order: wiki code, iso639_3, iso639_2B, iso639_1
    * @param code - the iso code
    * @return
    */
  def getByIsoCode(code: String) : Option[Language] = {
    get(code) match {
      case scala.None =>
        map.find(x => x._2.iso639_3 == code) match {
          case scala.None => map.find(x => x._2.iso639_2 == code) match {
            case scala.None => map.find(x => x._2.isoCode == code) match {
              case scala.None => scala.None
              case Some(l) => Some(l._2)
            }
            case Some(l) => Some(l._2)
          }
          case Some(l) => Some(l._2)
        }
      case Some(l) => Some(l)
    }
  }
  
  /**
   * Gets a language object for a Wikipedia language code, or the default if the given code is unknown.
   */
  def getOrElse(code: String, default: => Language) : Language = map.getOrElse(code, default)

}
