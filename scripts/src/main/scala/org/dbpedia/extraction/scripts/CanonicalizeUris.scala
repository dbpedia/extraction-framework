package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import scala.collection.mutable.{Set,HashMap,MultiMap,ArrayBuffer}
import java.io.File

/**
 * Maps old URIs in triple files to new URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 *   - only triples whose object URI has a certain domain are used
 * - read one or more files that need their URIs changed
 *   - the predicate is copied (and otherwise ignored)
 *   - triples containing URIs that cannot be mapped are discarded
 */
object CanonicalizeUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 9, 
      "need at least nine args: " +
      /*0*/ "base dir, " +
      /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), "+
      /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      /*3*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      /*4*/ "output dataset name extension (e.g. '-en-uris'), "+
      /*5*/ "input/output file suffix (e.g. '.nq.gz', '.ttl', '.ttl.bz2'), " +
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
    
    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val fileSuffix = args(5)
    require(fileSuffix.nonEmpty, "no input/output file suffix")
    
    // Language using generic domain (usually en)
    val generic = if (args(6) == "-") null else Language(args(6))
    
    def uriPrefix(language: Language): String = "http://"+(if (language == generic) "dbpedia.org" else language.dbpediaDomain)+"/resource/"      
    
    val newLanguage = Language(args(7))
    val newPrefix = uriPrefix(newLanguage)
    val newPredicateNs = newLanguage.propertyUri.append("")
    
    val languages = parseLanguages(baseDir, args.drop(8))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      
      val mappingFinder = new DateFinder(baseDir, language, mappingSuffix)
      
      // inter language links can link multiple articles between two languages, for example
      // http://de.wikipedia.org/wiki/Prostitution_in_der_Antike -> http://en.wikipedia.org/wiki/Prostitution_in_ancient_Rome
      // http://de.wikipedia.org/wiki/Prostitution_in_der_Antike -> http://en.wikipedia.org/wiki/Prostitution_in_ancient_Greece
      // TODO: in other cases, we probably want to treat multiple values as errors. Make this configurable.
      val map = new HashMap[String, Set[String]] with MultiMap[String, String]
      
      val reader = new QuadReader(mappingFinder)
      for (mappping <- mappings) {
        var count = 0
        reader.readQuads(mappping, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException("expected object uri, found object literal: "+quad)
          if (quad.value.startsWith(newPrefix)) {
            map.addBinding(quad.subject, quad.value)
            count += 1
          }
        }
        println("found "+count+" mappings")
      }
      
      // copy date, but use different suffix
      val fileFinder = new DateFinder(baseDir, language, fileSuffix, mappingFinder.date)
      
      val oldPrefix = uriPrefix(language)
      
      def newUris(oldUri: String): Set[String] = {
        if (oldUri.startsWith(oldPrefix)) map.getOrElse(oldUri, Set())
        else Set(oldUri) // not a DBpedia URI, copy it unchanged
      }
      
      val oldPredicateNs = language.propertyUri.append("")
      
      def newPredicate(oldPredicate: String): String = {
        if (oldPredicate.startsWith(oldPredicateNs)) newPredicateNs + oldPredicate.substring(oldPredicateNs.length)
        else oldPredicate // not a DBpedia URI, copy it unchanged
      }
      
      val mapper = new QuadMapper(fileFinder)
      for (input <- inputs) {
        mapper.mapQuads(input, input + extension, required = false) { quad =>
          val pred = newPredicate(quad.predicate)
          val subjects = newUris(quad.subject)
          if (subjects.isEmpty) {
            // no mapping for this subject URI - discard the quad. TODO: make this configurable
            List()
          }
          else if (quad.datatype == null) {
            // URI value - change subject and object URIs, copy everything else
            val objects = newUris(quad.value)
            if (objects.isEmpty) {
              // no mapping for this object URI - discard the quad. TODO: make this configurable
              List()
            } else {
              // map subject, predicate and object URI, copy everything else
              for (subj <- subjects; obj <- objects) yield quad.copy(subject = subj, predicate = pred, value = obj)
            }
          } else {
            // literal value - change subject and predicate URI, copy everything else
            for (subj <- subjects) yield quad.copy(subject = subj, predicate = pred)
          }
        }
      }
    }
    
  }
  
}
