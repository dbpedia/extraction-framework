package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,HashMap}

/**
 * Replace triples in a dataset by their transitive closure. All triples must use the same
 * property. Cycles are removed.
 */
object TransitiveClosure {
  
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

class TransitiveClosure(baseDir: File, language: Language, suffix: String) {

  private val finder = new Finder[File](baseDir, language)
  
  private var date: String = null
  
  private val uriMap = new HashMap[String, String]()
  
  def readTriples(input: String): Unit = {
    date = finder.dates(input).last
    val file = finder.file(date, input)
    println(language.wikiCode+": reading "+file+" ...")
    readLines(file) { line =>
      
    }
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