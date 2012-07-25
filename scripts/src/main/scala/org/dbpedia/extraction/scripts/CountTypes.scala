package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import java.io.File
import org.dbpedia.extraction.ontology.RdfNamespace.fullUri
import org.dbpedia.extraction.ontology.DBpediaNamespace.ONTOLOGY
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.util.RichFile.wrapFile
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.collection.Map
import IOUtils.write
import org.dbpedia.extraction.util.Finder
import scala.Console.err

class Counter(var num: Long = 0L)

/**
 * Example call:
 * ../run CountTypes /data/dbpedia instance-types .ttl.gz instance-types-counted.txt true 10000-
 */
object CountTypes {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 5,
      "need at least five args: "+
      /*0*/ "base dir, "+
      /*1*/ "input dataset name (e.g. 'instance-types'); "+
      /*2*/ "input file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'); " +
      /*3*/ "output text file name (e.g. 'instance-types-counted.txt' - one file per language dir, plus one file for total count in base dir); " +
      /*4*/ "boolean flag: if true, write total type count to file in base dir, otherwise don't; " +
      /*5*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val input = args(1)
    require(input.nonEmpty, "no input dataset")
    
    // Suffix of input file, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(2)
    require(suffix.nonEmpty, "no input file suffix")
    
    val output = args(3)
    require(output.nonEmpty, "no output file name")
    
    val total = args(4).toBoolean
    
    val languages = parseLanguages(baseDir, args.drop(5))
    require(languages.nonEmpty, "no languages")
    
    val rdfType = RdfNamespace.RDF.append("type")
    
    val totalTypes = if (total) new HashMap[String, Counter]() else null
    
    for (language <- languages) {
      val languageTypes = new HashMap[String, Counter]()
      val finder = new Finder(baseDir, language)
      val dates = finder.dates(input + suffix, required = false)
      if (dates.isEmpty) {
        err.println(language.wikiCode+": WARNING: no file ["+input+"] found")
      }
      else {
        val date = dates.last
        val file = finder.file(date, input + suffix)
        QuadReader.readQuads(language.wikiCode, file) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          if (quad.predicate != rdfType) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          countType(quad.value, languageTypes)
          if (total) countType(quad.value, totalTypes)
        }
        printTypes(finder.file(date, output), language.wikiCode+" "+input, languageTypes)
      }
    }
    if (total) printTypes(new File(baseDir, output), "total", totalTypes)
    
  }
  
  private def countType(uri: String, types: mutable.Map[String, Counter]): Unit = {
    types.getOrElseUpdate(uri, new Counter).num += 1
  }
  
  private def printTypes(file: File, tag: String, types: Map[String, Counter]): Unit = {
    val sorted = SortedMap(types.toArray: _*)
    val writer = write(file)
    try {
      for ((name, counter) <- sorted) {
        writer.write(name+"\t"+counter.num+"\n")
      }
    }
    finally writer.close()
  }
  
}
