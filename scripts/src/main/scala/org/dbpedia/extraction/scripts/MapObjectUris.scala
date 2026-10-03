package org.dbpedia.extraction.scripts

import java.io.File

import org.apache.jena.ext.com.google.common.collect.{Multimaps, TreeMultimap}
import org.dbpedia.extraction.config.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{DateFinder, Language, SimpleWorkers, Workers, RichFile, FileLike}

import scala.Console.err
import scala.collection.convert.decorateAsScala._

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
  *
  * Chile: added option (languages:@external) to use this script on datasets outside of language folders (like link sets) see param 6 and 7
 */
object MapObjectUris {

  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 7,
      "need at least seven args: " +
        /*0*/ "base dir of the extraction" +
        /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'transitive-redirects'), " +
        /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*3*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), " +
        /*4*/ "output dataset name extension (e.g. '-redirected'), " +
        /*5*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
        /*6*/ "languages or article count ranges (e.g. @downloaded, or 'en,fr' or '10000-') or " +
              "choose '@external' to map external datasets from a secondary directory (see last argument)" +
        /*7*/ "(optional) secondary directory (containing the input datasets to map if language option is '@external')")

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

    // Use all remaining args as keys or comma or whitespace separated lists of keys
    var isExternal = false
    val languages = if(args(6).trim == "@external") {
      isExternal = true
      Array(Language.English)
    }
    else
      parseLanguages(baseDir, split(args(6)))
    require(languages.nonEmpty, "no languages")

    val secondary = if(isExternal) new File(args(7)) else null

    // Redirects can have only one target, so we don't really need a MultiMap here.
    // But CanonicalizeUris also uses a MultiMap... TODO: Make this configurable.

    for (language <- languages) {
      val finder = new DateFinder(baseDir, language)
      val map = Multimaps.synchronizedSortedSetMultimap[String, String](TreeMultimap.create[String, String]())

        Workers.work(SimpleWorkers(1.5, 1.0) { mapping: String =>
        var count = 0
        new QuadMapper().readQuads(finder, mapping + mappingSuffix, auto = true, required = false) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(mapping + ": expected object uri, found object literal: " + quad)
          map.put(quad.subject, quad.value)
          count += 1
        }
        if (count > 0) err.println(mapping + ": found " + count + " mappings")
        else err.println(mapping + ": mapping file not found or empty, skipping for " + language.wikiCode)
      }, mappings.toList)

      Workers.work(SimpleWorkers(1.5, 1.0) { input: (String, String) =>
        var changeCount = 0
        val inputFileOption: Option[FileLike[_]] = if(isExternal) Some(new RichFile(new File(secondary, input._1 + input._2))) else finder.byName(input._1 + input._2, auto = true, required = false).map(x => x: FileLike[_])
        val outputFileOption: Option[FileLike[_]] = if(isExternal) Some(new RichFile(new File(secondary, input._1 + extension + input._2))) else finder.byName(input._1 + extension + input._2, auto = true, required = false).map(x => x: FileLike[_])

        if (inputFileOption.isDefined && outputFileOption.isDefined && inputFileOption.get.exists) {
          val inputFile = inputFileOption.get
          val outputFile = outputFileOption.get
          new QuadMapper().mapQuads(language, inputFile, outputFile) { quad =>
            if (quad.datatype != null) {
              // just copy quad with literal values. TODO: make this configurable
              List(quad)
            }
            else {
              val uris = map.get(quad.value).asScala
              changeCount = changeCount + 1
              val ret = for (uri <- uris)
                yield quad.copy(
                  value = uri, // change object URI
                  context = if (quad.context == null) quad.context else quad.context + "&objectMappedFrom=" + quad.value) // add change provenance
              // none found
              if(ret.isEmpty)
                List(quad)
              else
                ret
            }
          }
          err.println(input._1 + ": changed " + changeCount + " quads.")
        } else {
          err.println(input._1 + ": input file not found or empty, skipping for " + language.wikiCode)
        }
      }, inputs.flatMap(x => suffixes.map(y => (x, y))).toList)
    }
  }
}
