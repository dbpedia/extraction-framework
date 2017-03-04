package org.dbpedia.extraction.util

import java.io.{File, FileInputStream, InputStream, InputStreamReader}
import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, ObjectReader}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashSet
import scala.io.Codec
import scala.reflect.internal.MissingRequirementError
import scala.util.Try


object ConfigUtils {

  /**
    * Simple regex matching Wikipedia language codes.
    * Language codes have at least two characters, start with a lower-case letter and contain only
    * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
    */
  val LanguageRegex = """([a-z][a-z0-9-]+)""".r

  /**
    * Regex used for excluding languages from the import.
    */
  val ExcludedLanguageRegex = """!([a-z][a-z0-9-]+)""".r

  /**
    * Regex for numeric range, both limits optional
    */
  val RangeRegex = """(\d*)-(\d*)""".r

  val universalConfig = loadConfig(this.getClass.getClassLoader.getResource("universal.properties")).asInstanceOf[Properties]
  val baseDir = getValue(universalConfig , "base-dir", true){
    x => new File(x)
      //if (! dir.exists) throw error("dir "+dir+" does not exist")
      //dir
  }
  val wikiInfos = if(baseDir.exists())
    WikiInfo.fromFile(new File(baseDir, WikiInfo.FileName), Codec.UTF8)
  else
    Seq()

  def loadConfig(file: String, charset: String = "UTF-8"): Properties = {
    loadFromStream(new FileInputStream(file), charset)
  }

  def loadConfig(url: URL) = {

    url match {
      case selection => {
        if(selection.getFile.endsWith(".json"))
          loadJsonComfig(url)
        else
          loadFromStream(url.openStream())
      }
    }
  }

  def loadJsonComfig(url: URL): JsonNode ={
    val objectMapper = new ObjectMapper(new JsonFactory())
    val objectReader: ObjectReader = objectMapper.reader()
    val inputStream = url.openStream()
    val res = objectReader.readTree(inputStream)
    inputStream.close()
    res
  }

  private def loadFromStream(file: InputStream, charset: String = "UTF-8"): Properties ={
    val config = new Properties()
    try config.load(new InputStreamReader(file, charset))
    finally file.close()
    config
  }


  def getValues[T](config: Properties, key: String, sep: String, required: Boolean = false)(map: String => T): Seq[T] = {
    getStrings(config, key, sep, required).map(map(_))
  }

  def getStrings(config: Properties, key: String, sep: String, required: Boolean = false): Seq[String] = {
    val string = getString(config, key, required)
    if (string == null) Seq.empty
    else string.trimSplit(sep)
  }

  def getStringMap(config: Properties, key: String, sep: String, required: Boolean = false): Map[String, String] = {
    getStrings(config, key, sep, required).map(x => x.split("->")).map( y => y(0) -> y(1)).toMap
  }

  def getValue[T](config: Properties, key: String, required: Boolean = false)(map: String => T): T = {
    val string = getString(config, key, required)
    if (string == null) null.asInstanceOf[T]
    else map(string)
  }
  
  def getString(config: Properties, key: String, required: Boolean = false): String = {
    val string = config.getProperty(key)
    if (string != null) string
    else if (! required) null
    else throw new IllegalArgumentException("property '"+key+"' not defined.")
  }
  
  /**
   * @param baseDir directory of wikipedia.csv, needed to resolve article count ranges
   * @param args array of space- or comma-separated language codes or article count ranges
   * @return languages, sorted by language code
   */
  // TODO: reuse this in org.dbpedia.extraction.dump.download.DownloadConfig
  def parseLanguages(baseDir: File, args: Seq[String], wikiPostFix: String = "wiki"): Array[Language] = {
    
    val keys = for(arg <- args; key <- arg.split("[,\\s]"); if key.nonEmpty) yield key
        
    var languages = SortedSet[Language]()
    var excludedLanguages = SortedSet[Language]()
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case "@mappings" => languages ++= Namespace.mappings.keySet
      case "@chapters" => languages ++= Namespace.chapters.keySet
      case "@downloaded" => languages ++= downloadedLanguages(baseDir, wikiPostFix)
      case "@abstracts" => {
        //@downloaded - Commons & Wikidata
        languages ++= downloadedLanguages(baseDir, wikiPostFix)
        excludedLanguages += Language.Commons
        excludedLanguages += Language.Wikidata
      }
      case RangeRegex(from, to) => ranges += toRange(from, to)
      case LanguageRegex(language) => languages += Language(language)
      case ExcludedLanguageRegex(language) => excludedLanguages += Language(language)
      case other => throw new IllegalArgumentException("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      
      // for all wikis in one of the desired ranges...
      for ((from, to) <- ranges; wiki <- wikiInfos; if from <= wiki.pages && wiki.pages <= to)
      {
        // ...add its language
        Language.get(wiki.wikicode) match{
          case Some(l) => languages += l
          case None =>
        }
      }
    }

    languages --= excludedLanguages
    
    languages.toArray
  }

  private def downloadedLanguages(baseDir: File, wikiPostFix: String = "wiki"): Array[Language] = {
    for (file <- baseDir.listFiles().filter(x => x.isDirectory && x.getName.endsWith(wikiPostFix))) yield
      Language.get(file.getName.replace(wikiPostFix, "").replace("_", "-")) match{
        case Some(l) => l
        case None => throw new IllegalArgumentException(file.getName.replace(wikiPostFix, "").replace("_", "-") +
          ": is an unknown language code. Please update the addonlangs.json file and add this language.")
      }
  }

  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from.isEmpty) 0 else from.toInt
    val hi: Int = if (to.isEmpty) Int.MaxValue else to.toInt
    if (lo > hi) throw new NumberFormatException
    (lo, hi)
  }

  def parseVersionString(str: String): Try[String] =Try {
    Option(str) match {
      case Some(v) => "2\\d{3}-\\d{2}".r.findFirstMatchIn(v.trim) match {
        case Some(y) => if (y.end == 7) v.trim else throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
        case None => throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
      }
      case None => throw new IllegalArgumentException("No version string was provided.")
    }
  }
}