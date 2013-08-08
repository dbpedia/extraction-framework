package org.dbpedia.extraction.util

import java.util.Properties
import java.io.{File,FileInputStream,InputStreamReader}
import scala.collection.immutable.SortedSet
import org.dbpedia.extraction.util.RichString.wrapString
import scala.io.Codec
import org.dbpedia.extraction.wikiparser.Namespace
import scala.collection.mutable.{HashSet,HashMap}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering


/**
 * TODO: use scala.collection.Map[String, String] instead of java.util.Properties?
 */
object ConfigUtils {
  
  def loadConfig(file: String, charset: String): Properties = {
    val config = new Properties()
    
    val in = new FileInputStream(file)
    try config.load(new InputStreamReader(in, charset))
    finally in.close()
    
    config
  }
  
  def getValues[T](config: Properties, key: String, sep: Char, required: Boolean)(map: String => T): Seq[T] = {
    getStrings(config, key, sep, required).map(map(_))
  }

  def getStrings(config: Properties, key: String, sep: Char, required: Boolean): Seq[String] = {
    val string = getString(config, key, required)
    if (string == null) Seq.empty
    else string.trimSplit(sep)
  }

  def getValue[T](config: Properties, key: String, required: Boolean)(map: String => T): T = {
    val string = getString(config, key, required)
    if (string == null) null.asInstanceOf[T]
    else map(string)
  }
  
  def getString(config: Properties, key: String, required: Boolean): String = {
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
  // TODO: copy & paste in org.dbpedia.extraction.dump.download.DownloadConfig, org.dbpedia.extraction.dump.extract.Config
  def parseLanguages(baseDir: File, args: Seq[String]): Array[Language] = {
    
    var keys = for(arg <- args; key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
    var languages = SortedSet[Language]()
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case "@mappings" => languages ++= Namespace.mappings.keySet
      case RangeRegex(from, to) => ranges += toRange(from, to)
      case LanguageRegex(language) => languages += Language(language)
      case other => throw new IllegalArgumentException("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(baseDir, WikiInfo.FileName)
      
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
    
    languages.toArray
  }
  
  /**
   * Simple regex matching Wikipedia language codes.
   * Language codes have at least two characters, start with a lower-case letter and contain only 
   * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
   */
  val LanguageRegex = """([a-z][a-z0-9-]+)""".r
    
  /**
   * Regex for numeric range, both limits optional
   */
  val RangeRegex = """(\d*)-(\d*)""".r
  
  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from.isEmpty) 0 else from.toInt
    val hi: Int = if (to.isEmpty) Int.MaxValue else to.toInt
    if (lo > hi) throw new NumberFormatException
    (lo, hi)
  }
  
}