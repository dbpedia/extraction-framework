package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import java.io.File
import org.dbpedia.extraction.ontology.RdfNamespace.fullUri
import org.dbpedia.extraction.ontology.DBpediaNamespace.ONTOLOGY

class Counter(var num: Int = 0) {
  def ++ = num += 1
}

/**
 * Example call:
 * ../run CountTypes /data/dbpedia labels,short-abstracts,long-abstracts -fixed .nt.gz false 10000-
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
      /*3*/ "comma-separated type names (e.g. 'owl:Thing,Person,Place' - names without prefix are in DBpedia ontology), "+
      /*4*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val input = args(1)
    require(input.nonEmpty, "no input dataset")
    
    // Suffix of input file, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(2)
    require(suffix.nonEmpty, "no input file suffix")
    
    // split and trim type names and construct map from type URI to counter.
    val types = split(args(3)).map(_.trim).filter(_.nonEmpty).map(name => (fullUri(ONTOLOGY, name), new Counter)).toMap
    require(types.nonEmpty, "no type names")
    
    val languages = parseLanguages(baseDir, args.drop(4))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      val finder = new DateFinder(baseDir, language)
      new QuadReader(finder).readQuads(input + suffix, true) { quad =>
        if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
      }
    }
    
  }
  
}
