package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import org.dbpedia.extraction.util.RichFile
import java.net.{URI, URL}
import org.dbpedia.extraction.wikiparser.Namespace
import java.io.{Writer, File}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.util.RichFile._
import scala.collection.mutable.{ArrayBuffer, HashMap, MultiMap}
import scala.collection.mutable
import scala.Console._
import scala.Some
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.util.IOUtils._
import scala.Some
import scala.Some
import scala.collection.parallel.mutable.ParTrieMap
import scala.collection.concurrent.TrieMap

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

  // URI prefix for wikidata entities
  private val WikidataResource = "http://wikidata.dbpedia.org/resource/Q"

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 6,
      "need at least six args: " +
        /*0*/ "extraction config file" +
        /*1*/ "prefix of mapping file (eg. wikidata)"+
        /*2*/ "comma-separated names of datasets mapping URIs to wikidata identifiers (eg. wikidata-sameas)" +
        /*3*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*4*/ "comma-separated names of input datasets (e.g. 'infobox-properties,mappingbased-properties'), " +
        /*5*/ "output dataset name extension (e.g. '-normalized'), ")


    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    val baseDir = ConfigUtils.getValue(config, "base-dir", true)(new File(_))
    if (!baseDir.exists)
      throw new IllegalArgumentException("dir " + baseDir + " does not exist")
    val langConfString = ConfigUtils.getString(config, "languages", false)
    val languages = ConfigUtils.parseLanguages(baseDir, Seq(langConfString))
    require(languages.nonEmpty, "no languages")

    val formats = parseFormats(config, "uri-policy", "format")

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

    val extensions = List(outputExtension, outputExtension + "-subject-rejected", outputExtension + "-object-rejected", outputExtension + "-rejected")

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = formats.keys.map("." + _)
    require(suffixes.nonEmpty, "no input/output file suffixes")

    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val jobs = for(language <- languages) yield {
      val wikiFinder = new Finder(baseDir, language, "wiki") // Finds Wikipedia extracted datasets
      val date = wikiFinder.dates().last
      val datasets = for(input <- inputs; extension <- extensions) yield new Dataset(input + extension)
      val destination = createDestination(wikiFinder, date, formats, datasets)
      val mappingFinder = new DateFinder(baseDir, Language(mappingPrefix)) // Finds wikidata mapping dataset

      // We don't need a MultiMap here if dealing with one
      // But CanonicalizeUris also uses a MultiMap... TODO: Make this configurable.
      val map = new HashMap[String, mutable.Set[Int]] with MultiMap[String, Int]

      // QuadReader iterates through the whole mappings file every time?
      // Improve this with Akka or leave it to the Spark map-reduce version?
      for (mappping <- mappings) {
        var count = 0
        QuadReader.readQuads(mappingFinder, mappping + mappingSuffix, auto = true) {
          quad =>
            if (quad.datatype != null) throw new IllegalArgumentException(language.wikiCode + ": expected object uri, found object literal: " + quad)
            // TODO: this wastes a lot of space. Storing the part after ...dbpedia.org/resource/ would
            // be enough. Also, the fields of the Quad are derived by calling substring() on the whole
            // line, which means that the character array for the whole line is kept in memory, which
            // basically means that the whole redirects file is kept in memory. We should
            // - only store the resource title in the map
            // - use new String(quad.subject), new String(quad.value) to cut the link to the whole line
            // - maybe use an index of titles as in ProcessInterLanguageLinks to avoid storing duplicate titles

            //          // This checkLanguage manually extracts the host substring without creating a java.net.URI
            // Difference between performance: 9ms per line vs 8ms per line.
            //          def checkLanguage(quadObject: String): Boolean = {
            //            val startDomain = quadObject.substring(7) // part after http://
            //            val endDomain = startDomain.indexOf("/")
            //            val domain = if(endDomain == -1) startDomain else startDomain.substring(0, endDomain)
            //            val languagePrefix = if(language.wikiCode == "en") "" else language.wikiCode + "."
            //            if(domain.startsWith(languagePrefix + "dbpedia.org")) true // only read current language mappings
            //            else if(domain.endsWith("dbpedia.org")) false
            //            else true // accept non-dbpedia URIs
            //          }

            def checkLanguage(quadObject: URI): Boolean = {
              val domain = quadObject.getHost
              val languagePrefix = if (language.wikiCode == "en") "" else language.wikiCode + "."
              if (domain.startsWith(languagePrefix + "dbpedia.org")) true // only read current language mappings
              else if (domain.endsWith("dbpedia.org")) false
              else true // accept non-dbpedia URIs
            }

            if (quad.datatype != null || checkLanguage(new URI(quad.value))) {
              map.addBinding(quad.value, quad.subject.substring(WikidataResource.length).toInt) // Store just the Q-value integers
              count += 1
            }
        }
        err.println(language.wikiCode + ": found " + count + " mappings")
      }
      new NormalizationJob(language, destination, map, wikiFinder, inputs, suffixes.toSeq, extensions)
    }

    for(job <- jobs)
      job.run()

  }

  class NormalizationJob(language: Language, destination: Destination, map: HashMap[String, mutable.Set[Int]] with MultiMap[String, Int], wikiFinder: Finder[File], inputs: Seq[String], suffixes: Seq[String], extensions: Seq[String]) {
    val outputExtension = extensions(0)
    val subjectRejectExtension = extensions(1)
    val objectRejectExtension = extensions(2)
    val bothRejectExtension = extensions(3)

    val workers = SimpleWorkers(1.5, 1.0) {
      args: (Quad, String) =>
        val (quad, input) = args
        (map.get(quad.subject), map.get(quad.value)) match {
          case (Some(subIds), Some(objIds)) =>
            destination.write(for ((sub, obj) <- subIds zip objIds) yield quad.copy(subject = WikidataResource + sub, value = WikidataResource + obj,
              dataset = input + outputExtension))
          case (Some(ids), _) if quad.datatype != null || !new URI(quad.value).getHost.contains("dbpedia.org") =>
            destination.write(for (id <- ids) yield quad.copy(subject = WikidataResource + id,
              dataset = input + outputExtension)) // Keep triples with string literals non-dbpedia URIs in object
          case (None, Some(ids)) =>
            destination.write(Seq(quad.copy(dataset = input + subjectRejectExtension))) // Subject cannot be mapped in our URIs
          case (Some(ids), None) =>
            destination.write(Seq(quad.copy(dataset = input + objectRejectExtension))) // Object does not exist
          case (None, None) =>
            destination.write(Seq(quad.copy(dataset = input + bothRejectExtension)))
          case _ =>
        }
    }

    def run(): Unit = {
      for (input <- inputs; suffix <- suffixes) {
        val date = wikiFinder.dates().last
        val inFile: FileLike[File] = wrapFile(wikiFinder.file(date, input + suffix))

        try {
          destination.open()
          workers.start()

          QuadReader.readQuads(wikiFinder.language.wikiCode+": Reading triples from " + input + suffix, inFile) {
            quad => workers.process((quad, input))
          }

          workers.stop()
          destination.close()
        }
        catch {
          case e: Exception =>
            Console.err.println(e.printStackTrace())
        }
      }
    }
  }

  private def createDestination[T <% FileLike[T]](finder: Finder[T], date: String, formats: scala.collection.Map[String, Formatter], datasets: Seq[Dataset]) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[String, Destination]()
      for (dataset <- datasets) {
        val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
        datasetDestinations(dataset.name) = new WriterDestination(writer(file), format)
      }

      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination.toSeq: _*)
  }

  private def writer[T <% FileLike[T]](file: T): () => Writer = {
    () => IOUtils.writer(file)
  }
}
