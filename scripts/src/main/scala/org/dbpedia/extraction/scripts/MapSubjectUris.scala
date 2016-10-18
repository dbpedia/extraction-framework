package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import scala.collection.mutable
import scala.collection.mutable.{Set,HashMap,MultiMap}
import java.io.File
import scala.Console.err
import org.dbpedia.extraction.util.{Workers, DateFinder, SimpleWorkers, Language}

/**
 * Maps old subject URIs in triple files to new subject URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 * - read one or more files that need their subject URI changed:
 *   - the predicate is ignored
 *
 * Redirects in subject position SHOULD be resolved in the following datasets:
 *
 * anchor-text
 *
 * This is because during the extraction, the targets of the links are used as subjects in the anchor texts dataset
 * and thus no redirect resolving is performed on them.
 *
 * Example call:
 * ../run MapSubjectUris /data/dbpedia transitive-redirects .nt.gz anchor-text -redirected .nt.gz,.nq.gz 10000-
 *
 */
object MapSubjectUris {

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
        /*6*/ "languages or article count ranges (e.g. 'en,fr' or '10000-') or choose '@external' to map external datasets from a secondary directory (see last argument)" +
        /*7*/ "optional secondary directory (containing the input datasets to map if language option is '@external')")

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
    val languages = if (args(6).trim == "@external") {
      isExternal = true
      Array(Language.English)
    }
    else
      parseLanguages(baseDir, split(args(6)))
    require(languages.nonEmpty, "no languages")

    val secondary = if (isExternal) new File(args(7)) else null

    for (language <- languages) {
      val finder = new DateFinder(baseDir, language)
      val map = new HashMap[String, Set[String]] with MultiMap[String, String] with mutable.SynchronizedMap[String, Set[String]]

      Workers.work(SimpleWorkers(1.5, 1.0) { mapping: String =>
        var count = 0
        QuadReader.readQuads(finder, mapping + mappingSuffix, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(mapping + ": expected object uri, found object literal: " + quad)
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
        err.println(mapping + ": found " + count + " mappings")
      }, mappings.toList)


      Workers.work(SimpleWorkers(1.5, 1.0) { input: (String, String) =>
        var count = 0
        val inputFile = if (isExternal) new File(secondary, input._1 + input._2) else finder.byName(input._1 + input._2, auto = true).get
        val outputFile = if (isExternal) new File(secondary, input._1 + extension + input._2) else finder.byName(input._1 + extension + input._2, auto = true).get
        val tag = if (isExternal) input._1 else language.wikiCode
        QuadMapper.mapQuads(tag, inputFile, outputFile, required = true) { quad =>
          map.get(quad.subject) match {
            case Some(uris) => {
              count = count + 1
              for (uri <- uris)
                yield quad.copy(
                  subject = uri, // change object URI
                  context = if (quad.context == null) quad.context else quad.context + "&subjectMappedFrom=" + quad.subject) // add change provenance
            }
            case None => List(quad) // just copy quad without mapping for object URI. TODO: make this configurable
          }
        }
        err.println(input._1 + ": changed " + count + " quads.")
      }, inputs.flatMap(x => suffixes.map(y => (x, y))).toList)
    }
  }
}
