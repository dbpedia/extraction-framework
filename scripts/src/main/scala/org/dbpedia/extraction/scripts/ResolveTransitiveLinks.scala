package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,HashMap,Set,HashSet,ArrayBuffer}
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}

/**
 * Replace triples in a dataset by their transitive closure. All triples must use the same
 * property. Cycles are removed.
 */
object ResolveTransitiveLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && args.length >= 5, 
      "need at least five args: " +
      /*0*/ "base dir, " +
      /*1*/ "input file part (e.g. 'redirects'), " +
      /*2*/ "output file part (e.g. 'transitive-redirects'), " +
      /*3*/ "triples file suffix (e.g. '.nt.gz'), " +
      /*4*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val input = args(1)
    require(input.nonEmpty, "no input dataset name")
    
    val output = args(2)
    require(output.nonEmpty, "no output dataset name")
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .ttl and .nt files that use IRIs or URIs.
    // WARNING: DOES NOT WORK WITH .nq OR .tql.
    val fileSuffix = args(3)
    require(fileSuffix.nonEmpty, "no file suffix")
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args.drop(4))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
    }
      
  }

}

class ResolveTransitiveLinks(baseDir: File, language: Language, suffix: String) {

  private val finder = new Finder[File](baseDir, language)
  
  private var date: String = null
  
  private val uriMap = new HashMap[String, String]()
  
  private def find(part: String): File = {
    val name = part + suffix
    if (date == null) date = finder.dates(name).last
    finder.file(date, name)
  }
      
  /**
   * @param map file name part, e.g. redirects
   */
  def readMap(map: String): Unit = {
    val file = find(map)
    println(language.wikiCode+": reading "+file+" ...")
    var lineCount = 0
    var mapCount = 0
    val start = System.nanoTime
    var predicate: String = null
    readLines(file) { line =>
      line match {
        case Quad(quad) if (quad.datatype == null) => {
          if (predicate == null) predicate = quad.predicate
          else if (predicate != quad.predicate) throw new IllegalArgumentException("expected predicate "+predicate+", found "+quad.predicate+": "+line)
          uriMap(quad.subject) = quad.value
          mapCount += 1
        }
        case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: "+line)
      }
      lineCount += 1
      if (lineCount % 1000000 == 0) logRead(language.wikiCode, lineCount, start)
    }
    logRead(language.wikiCode, lineCount, start)
    println(language.wikiCode+": found "+mapCount+" URI mappings")
  }
  
  private def logRead(name: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  def resolve(): Unit = {
  }
  
  def writeTriples(output: String): Unit = {
    val file = finder.file(date, output)
    println(language.wikiCode+": writing "+file+" ...")
    val writer = write(file)
    try
    {
      
    }
    finally writer.close
  }
  
}
