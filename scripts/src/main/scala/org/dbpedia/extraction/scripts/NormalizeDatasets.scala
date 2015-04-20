package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import org.dbpedia.extraction.util.RichFile
import java.net.{URI, URL}
import org.dbpedia.extraction.wikiparser.Namespace
import java.io.File
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.util.RichFile._
import scala.collection.mutable.{HashMap,MultiMap}
import scala.collection.mutable
import scala.Console._
import scala.Some
import scala.Some

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
 * ../run MapSubjectUris /data/dbpedia wikidata wikidata-sameas .nt.gz infobox-properties,page-links,... -normalized .nt.gz,.nq.gz 10000-
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
    require(args != null && args.length >= 7,
      "need at least seven args: " +
        /*0*/ "extraction config file" +
        /*1*/ "prefix of mapping file (eg. wikidata)"+
        /*2*/ "comma-separated names of datasets mapping URIs to wikidata identifiers (eg. wikidata-sameas)" +
        /*3*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*4*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), " +
        /*5*/ "output dataset name extension (e.g. '-normalized'), " +
        /*6*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.bz2', '.ttl', '.ttl.bz2')")


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

    val mappingPrefix = args(1)

    val mappings = split(args(2))
    require(mappings.nonEmpty, "no mapping datasets")

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = args(3)
    require(mappingSuffix.nonEmpty, "no mapping file suffix")

    val inputs = split(args(4))
    require(inputs.nonEmpty, "no input datasets")

    val outputExtension = args(5)
    require(outputExtension.nonEmpty, "no output name extension")

    val subjectRejectExtension = outputExtension + "-subject-rejected"
    val objectRejectExtension = outputExtension + "-object-rejected"
    val bothRejectExtension = outputExtension + "-rejected"

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = split(args(6))
    require(suffixes.nonEmpty, "no input/output file suffixes")

    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val workers = SimpleWorkers(1.5, 1.0) { language: Language =>
      val wikiFinder = new DateFinder(baseDir, language) // Finds Wikipedia extracted datasets
      val mappingFinder = new DateFinder(baseDir, Language(mappingPrefix)) // Finds wikidata mapping dataset

      // Redirects can have only one target, so we don't really need a MultiMap here.
      // But CanonicalizeUris also uses a MultiMap... TODO: Make this configurable.
      val map = new HashMap[String, mutable.Set[String]] with MultiMap[String, String]

      // QuadReader iterates through the whole mappings file every time?
      // Improve this with Akka or leave it to the Spark map-reduce version?
      for (mappping <- mappings) {
        var count = 0
        QuadReader.readQuads(mappingFinder, mappping + mappingSuffix, auto = true) { quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(language.wikiCode+": expected object uri, found object literal: "+quad)
          // TODO: this wastes a lot of space. Storing the part after ...dbpedia.org/resource/ would
          // be enough. Also, the fields of the Quad are derived by calling substring() on the whole
          // line, which means that the character array for the whole line is kept in memory, which
          // basically means that the whole redirects file is kept in memory. We should
          // - only store the resource title in the map
          // - use new String(quad.subject), new String(quad.value) to cut the link to the whole line
          // - maybe use an index of titles as in ProcessInterLanguageLinks to avoid storing duplicate titles
          map.addBinding(quad.value, quad.subject)
          count += 1
        }
        err.println(language.wikiCode+": found "+count+" mappings")
      }

      for (input <- inputs; suffix <- suffixes) {
        val inFile: FileLike[File] = wrapFile(wikiFinder.find(input + suffix, auto = true))
        val outFiles = for(extension <- List(outputExtension, subjectRejectExtension, objectRejectExtension, bothRejectExtension))
                        yield wrapFile(wikiFinder.find(input + extension + suffix, auto = true))

        QuadMapper.mapQuads(wikiFinder.language.wikiCode, inFile.asInstanceOf[FileLike[File]], outFiles, required = false, quads = false, turtle = true) {
          quad =>
            (map.get(quad.subject), map.get(quad.value)) match {
              case (Some(subUris), Some(objUris)) =>
                // Quads going into outputExtension dataset
                Seq(for ((sub, obj) <- subUris zip objUris) yield quad.copy(subject = sub, value = obj),
                  Nil, Nil, Nil)
              case (Some(uris), _) if quad.datatype != null || !new URI(quad.value).getHost.contains("dbpedia.org") =>
                // Quads going into outputExtension dataset
                Seq(for (uri <- uris) yield quad.copy(subject = uri),
                  Nil, Nil, Nil) // Keep triples with string literals non-dbpedia URIs in object
              case (None, Some(uris)) => Seq(Nil, List(quad), Nil, Nil) // Subject cannot be mapped in our URIs
              case (Some(uris), None) => Seq(Nil, Nil, List(quad), Nil) // Object does not exist
              case (None, None) => Seq(Nil, Nil, Nil, List(quad))
              case _ => Seq(Nil, Nil, Nil, Nil)
            }
        }
      }
    }

    workers.start()
    for (language <- languages) workers.process(language)
    workers.stop()

  }
}
