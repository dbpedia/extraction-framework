package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.LinkedHashMap
import java.io.File

/**
 * Replace triples in a dataset by their transitive closure.
 * All triples must use the same property. Cycles are removed.
 *
 * Example call:
 * ../run ResolveTransitiveLinks /data/dbpedia redirects transitive-redirects .nt.gz 10000-
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
    require(output != input, "output dataset name must different from input dataset name ")
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt or .ttl files, using IRIs or URIs.
    // Does NOT work with .nq or .tql files. (Preserving the context wouldn't make sense.)
    val suffix = args(3)
    require(suffix.nonEmpty, "no file suffix")
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args.drop(4))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      
      val finder = new DateFinder(baseDir, language)
      
      // use LinkedHashMap to preserve order
      val map = new LinkedHashMap[String, String]()
      
      var predicate: String = null
      QuadReader.readQuads(finder, input + suffix, auto = true) { quad =>
        if (quad.context != null) throw new IllegalArgumentException("expected triple, found quad: "+quad)
        if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
        if (predicate == null) predicate = quad.predicate
        else if (predicate != quad.predicate) throw new IllegalArgumentException("expected predicate "+predicate+", found "+quad.predicate+": "+quad)
        map(quad.subject) = quad.value
      }

      println("resolving "+map.size+" links...")
      val cycles = new TransitiveClosure(map).resolve()
      println("found "+cycles.size+" cycles:")
      for (cycle <- cycles.sortBy(- _.size)) {
        println("length "+cycle.size+": ["+cycle.mkString(" ")+"]")
      }
      
      val file = finder.find(output + suffix)
      println(language.wikiCode+": writing "+file+" ...")
      val writer = write(file)
      try {
        for ((subjUri, objUri) <- map) {
          writer.write("<"+subjUri+"> <"+predicate+"> <"+objUri+"> .\n")
        }
      }
      finally writer.close
    }
      
  }

}
