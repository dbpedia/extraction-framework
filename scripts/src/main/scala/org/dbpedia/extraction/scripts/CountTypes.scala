package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import java.io.File
import org.dbpedia.extraction.ontology.RdfNamespace.fullUri
import org.dbpedia.extraction.ontology.DBpediaNamespace.ONTOLOGY
import org.dbpedia.extraction.ontology.RdfNamespace
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.collection.Map

class Counter(var num: Int = 0)

/**
 * Example call:
 * ../run CountTypes /data/dbpedia instance-types .ttl.gz 10000-
 */
object CountTypes {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 4, 
      "need at least four args: "+
      /*0*/ "base dir, "+
      /*1*/ "name of input dataset (e.g. 'instance-types'), "+
      /*2*/ "input file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*3*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val input = args(1)
    require(input.nonEmpty, "no input dataset")
    
    // Suffix of input file, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(2)
    require(suffix.nonEmpty, "no input file suffix")
    
    val languages = parseLanguages(baseDir, args.drop(3))
    require(languages.nonEmpty, "no languages")
    
    val rdfType = RdfNamespace.RDF.append("type")
    
    val totalTypes = new HashMap[String, Counter]()
    
    for (language <- languages) {
      val languageTypes = new HashMap[String, Counter]()
      val finder = new DateFinder(baseDir, language)
      QuadReader.readQuads(finder, input + suffix, auto = true) { quad =>
        if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
        if (quad.predicate != rdfType) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
        countType(quad.value, languageTypes)
        countType(quad.value, totalTypes)
      }
      printTypes(language.wikiCode+" "+input, languageTypes)
    }
    printTypes("total", totalTypes)
    
  }
  
  private def countType(uri: String, types: mutable.Map[String, Counter]): Unit = {
    types.getOrElseUpdate(uri, new Counter).num += 1
  }
  
  private def printTypes(tag: String, types: Map[String, Counter]): Unit = {
    val sorted = SortedMap(types.toArray: _*)
    println(tag)
    for ((name, counter) <- sorted) {
      println(name+"\t"+counter.num)
    }
    println()
  }
  
}
