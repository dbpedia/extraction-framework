package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util.{SimpleWorkers, ConfigUtils, Language}
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import java.net.URI
import org.dbpedia.extraction.wikiparser.Namespace
import java.io.File
import org.dbpedia.extraction.util.ConfigUtils._
import scala.collection.mutable.{Set,HashMap,MultiMap}
import scala.collection.mutable
import scala.Predef.Set
import scala.Console._
import scala.Some
import org.dbpedia.extraction.destinations.Quad

/**
 * Normalize all triples into the new namespace using wikidata identifiers as base using
 * sameAs links between different projects.
 *
 * - read one or more triple files that contain the URI mapping (wikidata sameAs triples):
 *   - the predicate is ignored (only read owl:sameAs links and ignore the rest?)
 * - read one or more files that need to be normalized into the new namespace:
 *   - the predicate is ignored
 *
 * Example call:
 * ../run MapSubjectUris /data/dbpedia wikidata-sameas .nt.gz infobox-properties,page-links,... -normalized .nt.gz,.nq.gz 10000-
 *
 * The output name extension for triples, that are rejected because the subject does not exist, becomes
 * \-normalized-subject-rejected, * and similarly for triples with non-existant objects it becomes -normalized-object-rejected,
 * where -normalized is the provided * output extension. Triples that have both subject and object missing from the mapping
 * are sent to the extension -normalized-rejected. No changes are made to rejected triples.
 *
 */

object NormalizeDatasets {

  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 6,
      "need at least six args: " +
        /*0*/ "extraction config file" +
        /*1*/ "comma-separated names of datasets mapping URIs to wikidata identifiers (eg. wikidata-sameas)"+
        /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*3*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), "+
        /*4*/ "output dataset name extension (e.g. '-normalized'), "+
        /*5*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2')")


    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    val baseDir = ConfigUtils.getValue(config, "base-dir", true)(new File(_))
    if (!baseDir.exists)
      throw new IllegalArgumentException("dir " + baseDir + " does not exist")
    val langConfString = ConfigUtils.getString(config, "languages", false)
    val languages = ConfigUtils.parseLanguages(baseDir, Seq(langConfString))
    require(languages.nonEmpty, "no languages")

    val formats = parseFormats(config, "uri-policy", "format")

    lazy val ontology = {
      val ontologyFile = ConfigUtils.getValue(config, "ontology", false)(new File(_))
      val ontologySource = if (ontologyFile != null && ontologyFile.isFile) {
        XMLSource.fromFile(ontologyFile, Language.Mappings)
      }
      else {
        val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
        val url = new URL(Language.Mappings.apiUri)
        WikiSource.fromNamespaces(namespaces, url, Language.Mappings)
      }

      new OntologyReader().read(ontologySource)
    }

    val mappings = split(args(1))
    require(mappings.nonEmpty, "no mapping datasets")

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = args(2)
    require(mappingSuffix.nonEmpty, "no mapping file suffix")

    val inputs = split(args(3))
    require(inputs.nonEmpty, "no input datasets")

    val extension = args(4)
    require(extension.nonEmpty, "no output name extension")

    val subjectRejectExtension = extension + "-subject-rejected"
    val objectRejectExtension = extension + "-object-rejected"
    val bothRejectExtension = extension + "-rejected"

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = split(args(5))
    require(suffixes.nonEmpty, "no input/output file suffixes")

    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val workers = SimpleWorkers(1.5, 1.0) { language: Language =>

      val finder = new DateFinder(baseDir, language)

      // Redirects can have only one target, so we don't really need a MultiMap here.
      // But CanonicalizeUris also uses a MultiMap... TODO: Make this configurable.
      val map = new HashMap[String, mutable.Set[String]] with MultiMap[String, String]

      // QuadReader iterates through the whole mappings file every time?
      // Improve this with Akka or leave it to the Spark map-reduce version?
      for (mappping <- mappings) {
        var count = 0
        QuadReader.readQuads(finder, mappping + mappingSuffix, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(language.wikiCode+": expected object uri, found object literal: "+quad)
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
        err.println(language.wikiCode+": found "+count+" mappings")
      }

      case class NonDbpedia

      // Anonymous case function to modify the triples depending upon presence or absence of subjects/objects.
      val matcher: (Quad, Option[mutable.Set[String]], Option[mutable.Set[String]]) => Traversable[Quad] = {
        case (Some(subjectUris), Some(valueUris)) =>
          for (uri1 <- subjectUris; uri2 <- valueUris) yield quad.copy(subject = uri1, value = uri2)
        case (Some(uris), NonDbpedia) =>
          for (uri <- uris) yield quad.copy(subject = uri)
        case (Some(uris), None) => List(quad)
        case (None, Some(uris)) => List(quad)
        case (None, None) => List(quad)
        case _ => Nil
      }

      for (input <- inputs; suffix <- suffixes) {
        for(extension <-List(extension, subjectRejectExtension, objectRejectExtension, bothRejectExtension, none)) {
          QuadMapper.mapQuads(finder, input + suffix, input + extension + suffix, required = false) {
            quad =>
              if (quad.datatype != null) List(quad) // just copy quad with literal values. TODO: make this configurable
              else {
                val valueUri = new URI(quad.value)
                // Keep non-dbpedia URIs
                if(valueUri.contains("dbpedia.org")) matcher(quad, map.get(quad.subject), NonDbpedia)
                else matcher(map.get(quad.subject), map.get(quad.value))
              }
          }
        }
      }
    }

    workers.start()
    for (language <- languages) workers.process(language)
    workers.stop()

  }
}
