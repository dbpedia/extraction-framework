package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import scala.collection.mutable.{Set,HashMap,MultiMap}
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
 * disambiguations
 * infobox-properties
 * mappingbased-properties
 * page-links
 * persondata
 * topical-concepts
 * 
 * Redirects seem to be so rare in categories that it doesn't make sense to resolve these:
 * 
 * article-categories
 * skos-categories
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
 * specific-mappingbased-properties
 * 
 * Maybe we should resolve redirects in interlanguage-links, but we would have to integrate
 * redirect resolution into interlanguage link resolution. We're pretty strict when we generate
 * interlanguage-links-same-as. If we resolve redirects in interlanguage-links, we would probably
 * gain a few interlanguage-links (tenths of a percent), but we would not eliminate errors.
 * 
 * Example call:
 * ../run MapObjectUris /data/dbpedia transitive-redirects .nt.gz infobox-properties,mappingbased-properties,... -redirected .nt.gz,.nq.gz 10000-
 * 
 * The following should be redirected (as of 2012-07-11)
 * disambiguations,infobox-properties,mappingbased-properties,page-links,persondata,topical-concepts
 * (specific-mappingbased-properties is not necessary, it has only literal values)
 * 
 * TODO: merge with CanonicalizeUris?
 */
object MapObjectUris {
  
  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 7, 
      "need at least seven args: " +
      /*0*/ "base dir, " +
      /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'transitive-redirects'), "+
      /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*3*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), "+
      /*4*/ "output dataset name extension (e.g. '-redirected'), "+
      /*5*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
      /*6*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val mappings = split(args(1))
    require(mappings.nonEmpty, "no mapping datasets")
    
    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = args(2)
    require(mappingSuffix.nonEmpty, "no mapping file suffix")
    
    val inputs = split(args(3))
    require(inputs.nonEmpty, "no input datasets")
    
    val extension = args(4)
    require(extension.nonEmpty, "no result name extension")
    
    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val fileSuffixes = split(args(5))
    require(fileSuffixes.nonEmpty, "no input/output file suffixes")
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args.drop(6))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      
      val mappingFinder = new DateFinder(baseDir, language, mappingSuffix)
      
      // Redirects can have only one target, so we don't really need a MultiMap here.
      // But CanonicalizeUris also uses a MultiMap... TODO: Make this configurable.
      val map = new HashMap[String, Set[String]] with MultiMap[String, String]
      
      val reader = new QuadReader(mappingFinder)
      for (mappping <- mappings) {
        var count = 0
        reader.readQuads(mappping, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          // TODO: this wastes a lot of space. Storing the part after ...dbpedia.org/resource/ would
          // be enough. Also, the fields of the Quad are derived by calling substring() on the whole 
          // line, which means that the character array for the whole line is kept in memory, which
          // basically means that the whole redirects file is kept in memory. We should
          // - only store the resource title in the map
          // - use new String(quad.subject), new String(quad.value) to cut the link to the whole line
          // - maybe use an index of titles as in ProcessInterLanguageLinks to avoid storing duplicate titles
          map.addBinding(quad.subject, quad.value)
          count += 1
        }
        println("found "+count+" mappings")
      }
      
      for (fileSuffix <- fileSuffixes) {
        // copy date, but use different suffix
        val fileFinder = new DateFinder(baseDir, language, fileSuffix, mappingFinder.date)
        
        val mapper = new QuadMapper(fileFinder)
        for (input <- inputs) {
          mapper.mapQuads(input, input + extension, required = false) { quad =>
            if (quad.datatype != null) List(quad) // just copy quad with literal values. TODO: make this configurable
            else map.get(quad.value) match {
              case Some(uris) => for (uri <- uris) yield quad.copy(value = uri) // change object URI
              case None => List(quad) // just copy quad without mapping for object URI. TODO: make this configurable
            }
          }
        }
      }
      
    }
    
  }
  
}
