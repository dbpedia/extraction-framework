package org.dbpedia.extraction.util

import java.io.File
import scala.collection.immutable.SortedSet
import scala.io.Codec
import org.dbpedia.extraction.util
import org.dbpedia.extraction.wikiparser.Namespace
import scala.collection.mutable.{HashSet,HashMap}

object ConfigUtils {
  
  /**
   * @param baseDir directory of wikipedia.csv, needed to resolve article count ranges
   * @param args array of space- or comma-separated language codes or article count ranges
   * @return languages, sorted by language code
   */
  // TODO: copy & paste in org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
  def parseLanguages(baseDir: File, args: Array[String]): Array[util.Language] = {
    
    var keys = for(arg <- args; key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
    var languages = SortedSet[Language]()(util.Language.wikiCodeOrdering)
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case "@mappings" => languages ++= Namespace.mappings.keySet
      case Range(from, to) => ranges += toRange(from, to)
      case Language(language) => languages += util.Language(language)
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
        languages += util.Language(wiki.language)
      }
    }
    
    languages.toArray
  }
  
  /**
   * Simple regex matching Wikipedia language codes.
   * Language codes have at least two characters, start with a lower-case letter and contain only 
   * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
   */
  val Language = """([a-z][a-z0-9-]+)""".r
    
  /**
   * Regex for numeric range, both limits optional
   */
  val Range = """(\d*)-(\d*)""".r
  
  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from isEmpty) 0 else from.toInt
    val hi: Int = if (to isEmpty) Int.MaxValue else to.toInt
    if (lo > hi) throw new NumberFormatException
    (lo, hi)
  }
  
}