package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import scala.collection.mutable.HashMap
import java.io.File

/**
 * Maps old object URIs in triple files to new object URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 * - read one or more files that need their object URI changed:
 *   - the predicate is ignored
 *   - literal values and quads without mapping for object URI are copied unchanged
 * 
 * Redirects SHOULD be resolved in the following datasets:
 * 
 * article-categories
 * disambiguations
 * infobox-properties
 * mappingbased-properties
 * specific-mappingbased-properties
 * page-links
 * persondata
 * skos-categories
 * topical-concepts
 * 
 * The following datasets DO NOT have object URIs that can be redirected:
 * 
 * category-labels
 * external-links
 * flickr-wrappr-links
 * geo-coordinates
 * homepages
 * images
 * infobox-property-definitions
 * infobox-test
 * instance-types
 * iri-same-as-uri
 * labels
 * 
 * Maybe we should resolve redirects in interlanguage-links, but we would have to integrate
 * redirect resolution into interlanguage link resolution. We're pretty strict when we generate
 * interlanguage-links-same-as. If we resolve redirects in interlanguage-links, we would probably
 * gain a few interlanguage-links (tenths of a percent), but we would not eliminate errors.
 */
object MapObjectUris {
  
  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 6, 
      "need at least six args: " +
      /*0*/ "base dir, " +
      /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'transitive-redirects'), "+
      /*2*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), "+
      /*3*/ "result dataset name extension (e.g. '-redirected'), "+
      /*4*/ "triples file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*5*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
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
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args.drop(5))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      
      val map = new HashMap[String, String]()
      
      val reader = new QuadReader(baseDir, language, suffix)
      for (mappping <- mappings) {
        var count = 0
        reader.readQuads(mappping) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          map(quad.subject) = quad.value
          count += 1
        }
        println("found "+count+" mappings")
      }
      
      val mapper = new QuadMapper(reader)
      for (input <- inputs) {
        mapper.mapQuads(input, input + extension, required = false) { quad =>
          if (quad.datatype == null) Some(quad) // just copy quad with literal values
          else map.get(quad.value) match {
            case Some(uri) => Some(quad.copy(value = uri)) // change object URI
            case None => Some(quad) // just copy quad without mapping for object URI
          }
        }
      }
    }
    
  }
  
}
