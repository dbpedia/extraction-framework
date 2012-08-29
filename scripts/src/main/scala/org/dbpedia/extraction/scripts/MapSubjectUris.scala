package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import scala.collection.mutable.{Set,HashMap,MultiMap}
import java.io.File

/**
 * Maps old URIs in triple files to new URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 *   - only triples whose object URI has a certain domain are used
 * - read one or more files that need their subject URI changed
 *   - the predicate is ignored
 */
object MapSubjectUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 7, 
      "need at least seven args: " +
      /*0*/ "base dir, " +
      /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), "+
      /*2*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      /*3*/ "result dataset name extension (e.g. '-en-uris'), "+
      /*4*/ "triples file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*5*/ "new URI domain (e.g. 'en.dbpedia.org' or 'dbpedia.org'), " +
      /*6*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val mappings = split(args(1))
    require(mappings.nonEmpty, "no mapping datasets")
    
    val inputs = split(args(2))
    require(inputs.nonEmpty, "no input datasets")
    
    val extension = args(3)
    require(extension.nonEmpty, "no result name extension")
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(4)
    require(suffix.nonEmpty, "no file suffix")
    
    val domain = "http://"+args(5)+"/"
    require(domain != "http:///", "no new domain")
    
    val languages = parseLanguages(baseDir, args.drop(6))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      
      // inter language links can link multiple articles between two languages, for example
      // http://de.wikipedia.org/wiki/Prostitution_in_der_Antike -> http://en.wikipedia.org/wiki/Prostitution_in_ancient_Rome
      // http://de.wikipedia.org/wiki/Prostitution_in_der_Antike -> http://en.wikipedia.org/wiki/Prostitution_in_ancient_Greece
      // TODO: in other cases, we probably want to treat multiple values as errors. Make this configurable.
      val map = new HashMap[String, Set[String]] with MultiMap[String, String]
      
      val reader = new QuadReader(baseDir, language, suffix)
      for (mappping <- mappings) {
        var count = 0
        reader.readQuads(mappping) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          if (quad.value.startsWith(domain)) {
            map.addBinding(quad.value, quad.subject)
            count += 1
          }
        }
        println("found "+count+" mappings")
      }
      
      val mapper = new QuadMapper(reader)
      for (input <- inputs) {
        mapper.mapQuads(input, input + extension, required = false) { quad =>
          map.get(quad.subject) match {
            case Some(uris) => for (uri <- uris) yield quad.copy(subject = uri) // change subject URI
            case None => List() // no mapping for this subject URI - discard the quad. TODO: make this configurable
          }
        }
      }
    }
    
  }
  
}
