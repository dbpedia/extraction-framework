package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.SimpleWorkers
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import scala.collection.mutable.{Set,HashMap,MultiMap,ArrayBuffer}
import java.io.File
import scala.Console.err

/**
 * Maps old URIs in triple files to new URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 *   - only triples whose object URI has the target domain are used
 * - read one or more files that need their URIs changed
 *   - DBpedia URIs in subject, predicate or object position are mapped
 *   - non-DBpedia URIs and literal values are copied unchanged
 *   - triples containing DBpedia URIs in subject or object position that cannot be mapped are discarded
 * 
 * As of DBpedia release 3.8 and 3.9, the following datasets should be canonicalized:
 * 
 * article-categories
 * category-labels
 * disambiguations
 * disambiguations-redirected
 * external-links
 * geo-coordinates
 * homepages
 * images
 * infobox-properties
 * infobox-properties-redirected
 * infobox-property-definitions
 * instance-types
 * labels
 * long-abstracts
 * mappingbased-properties
 * mappingbased-properties-redirected
 * page-in-link-counts (redirected)
 * page-links
 * page-links-redirected
 * page-out-link-counts (redirected)
 * persondata
 * persondata-redirected
 * pnd
 * short-abstracts
 * skos-categories
 * specific-mappingbased-properties
 * 
 * Example calls:
 * ../run CanonicalizeUris /data/dbpedia interlanguage-links .nt.gz labels,short-abstracts,long-abstracts -en-uris .nt.gz,.nq.gz en en 10000-
 * 
 * ../run CanonicalizeUris /data/dbpedia interlanguage-links .nt.gz article-categories,category-labels,disambiguations,disambiguations-redirected,external-links,geo-coordinates,homepages,images,infobox-properties,infobox-properties-redirected,infobox-property-definitions,instance-types,labels,long-abstracts,mappingbased-properties,mappingbased-properties-redirected,page-links,page-in-link-counts,page-in-link-counts-redirected,page-links,page-links-redirected,page-out-link-counts,persondata,persondata-redirected,pnd,short-abstracts,skos-categories,specific-mappingbased-properties -en-uris .nt.gz,.nq.gz en en 10000-
 * 
 * TODO: merge with MapObjectUris?
 */
object CanonicalizeUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 9, 
      "need at least nine args: " +
      /*0*/ "base dir, " +
      /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), "+
      /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*3*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      /*4*/ "output dataset name extension (e.g. '-en-uris'), "+
      /*5*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
      /*6*/ "wiki code of generic domain (e.g. 'en', use '-' to disable), " +
      /*7*/ "wiki code of new URIs (e.g. 'en'), " +
      /*8*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
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
    val suffixes = split(args(5))
    require(suffixes.nonEmpty, "no input/output file suffixes")
    
    // Language using generic domain (usually en)
    val generic = if (args(6) == "-") null else Language(args(6))
    
    def uriPrefix(language: Language): String = "http://"+(if (language == generic) "dbpedia.org" else language.dbpediaDomain)+"/"      
    
    val newLanguage = Language(args(7))
    
    val newPrefix = uriPrefix(newLanguage)
    val newResource = newPrefix+"resource/"
    
    val languages = parseLanguages(baseDir, args.drop(8))
    require(languages.nonEmpty, "no languages")
    
    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val workers = SimpleWorkers(1.5, 1.0) { language: Language =>
      
      val oldPrefix = uriPrefix(language)
      val oldResource = oldPrefix+"resource/"
      
      val finder = new DateFinder(baseDir, language)
      
      val map = new HashMap[String, String]
      
      for (mappping <- mappings) {
        var count = 0
        QuadReader.readQuads(finder, mappping + mappingSuffix, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(language.wikiCode+": expected object uri, found object literal: "+quad)
          if (quad.value.startsWith(newResource)) {
            // TODO: this wastes a lot of space. Storing the part after ...dbpedia.org/resource/ would
            // be enough. Also, the fields of the Quad are derived by calling substring() on the whole 
            // line, which may mean that the character array for the whole line is kept in memory, which
            // basically means that the whole redirects file is kept in memory. We should
            // - only store the resource title in the map
            // - use new String(quad.subject), new String(quad.value) to cut the link to the whole line
            // - maybe use an index of titles as in ProcessInterLanguageLinks to avoid storing duplicate titles
            // TODO: check that there was no previous mapping?
            map(quad.subject) = quad.value
            count += 1
          }
        }
        err.println(language.wikiCode+": found "+count+" mappings")
      }
      
      def newUri(oldUri: String): String = {
        if (oldUri.startsWith(oldPrefix)) newPrefix + oldUri.substring(oldPrefix.length)
        else oldUri // not a DBpedia URI, copy it unchanged
      }
      
      def mapUri(oldUri: String): String = {
        if (oldUri.startsWith(oldResource)) map.getOrElse(oldUri, null)
        else newUri(oldUri)
      }
      
      for (input <- inputs; suffix <- suffixes) {
        QuadMapper.mapQuads(finder, input + suffix, input + extension + suffix, required = false) { quad =>
          val pred = newUri(quad.predicate)
          val subj = mapUri(quad.subject)
          if (subj == null) {
            // no mapping for this subject URI - discard the quad. TODO: make this configurable
            List()
          }
          else if (quad.datatype == null) {
            // URI value - change subject and object URIs, copy everything else
            val obj = mapUri(quad.value)
            if (obj == null) {
              // no mapping for this object URI - discard the quad. TODO: make this configurable
              List()
            } else {
              // map subject, predicate and object URI, copy everything else
              List(quad.copy(subject = subj, predicate = pred, value = obj))
            }
          } else {
            // literal value - change subject and predicate URI, copy everything else
            List(quad.copy(subject = subj, predicate = pred))
          }
        }
      }
      
    }
    
    workers.start()
    
    for (language <- languages) {
      // mapping the target language to itself doesn't make sense
      if (language != newLanguage) {
        workers.process(language)
      }
    }
    
    workers.stop()
  }
  
}
