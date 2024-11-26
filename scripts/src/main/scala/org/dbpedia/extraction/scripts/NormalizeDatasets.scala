package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import org.dbpedia.extraction.util.RichFile
import java.net.{URI, URL}
import org.dbpedia.extraction.wikiparser.Namespace
import java.io._
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
import java.util.logging.{Logger, Level}
import scala.Some
import scala.Console
import SortedDestination._
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
 * ../run NormalizeDatasets /data/dbpedia wikidata wikidata-sameas .ttl.bz2 mappingbased-properties,page-links,... -normalized
 *
 * The output name extension for triples, that are rejected because the subject does not exist, becomes
 * \-normalized-subject-rejected, * and similarly for triples with non-existant objects it becomes -normalized-object-rejected,
 * where -normalized is the provided * output extension. Triples that have both subject and object missing from the mapping
 * are sent to the extension -normalized-rejected. No changes are made to rejected triples.
 *
 */

object NormalizeDatasets {

  private val logger = Logger.getLogger(classOf[NormalizationJob].getName)

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
        /*4*/ "comma-separated names of input datasets (e.g. 'mappingbased-properties,page-links'), " +
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

    val inputs = split(args(4)).map(_.replace("_", "-"))
    require(inputs.nonEmpty, "no input datasets")

    val outputExtension = args(5)
    require(outputExtension.nonEmpty, "no output name extension")

    val extensions = List(outputExtension, outputExtension + "-subject-rejected", outputExtension + "-object-rejected", outputExtension + "-rejected")

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = formats.keys.map("." + _)
    require(suffixes.nonEmpty, "no input/output file suffixes")

    val mappingFinder = new DateFinder(baseDir, Language(mappingPrefix)) // Finds wikidata mapping dataset

    // Generate the sorted wikidata mappings file only once
    for(mapping <- mappings) {
      val originalFile = mappingFinder.find(mapping + mappingSuffix, auto = true)
      val sortedFile = mappingFinder.find(mapping+"-sorted"+mappingSuffix, auto = true)
      if(!sortedFile.exists()) {
        logger.info(s"Sorting $originalFile into $sortedFile...")
        sort(originalFile, sortedFile)
      }
    }

    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val jobs = for(language <- languages) yield {
      val wikiFinder = new Finder(baseDir, language, "wiki") // Finds Wikipedia extracted datasets
      val date = wikiFinder.dates().last
      val datasets = for(input <- inputs; extension <- extensions) yield new Dataset(input + extension)
      val destination = createDestination(wikiFinder, date, formats, datasets)

      val map = {
        // Try reading from cache
        val cache = wikiFinder.file(date, "sameas-mappings.obj")
        logger.info("Loading sameas mappings from cache file "+cache)
        try
        {
          loadMappingsFromCache(cache)
        }
        catch
        {
          case ex : Exception => logger.log(Level.INFO, "Will extract redirects from source for "+language.wikiCode+" wiki, could not load cache file '"+cache+"': "+ex)
            val map = loadMappingsFromSource(language, mappingFinder, mappings, mappingSuffix)

            // Save mappings to cache
            val dir = cache.getParentFile
            if (! dir.exists && ! dir.mkdirs) throw new IOException("cache dir ["+dir+"] does not exist and cannot be created")
            val outputStream = new ObjectOutputStream(new FileOutputStream(cache))
            try
            {
              outputStream.writeObject(map)
            }
            finally
            {
              outputStream.close()
            }
            logger.info(map.size + " sameas mappings written to cache file "+cache)
            map
        }
      }
      new NormalizationJob(language, destination, map.toMap, wikiFinder, inputs, suffixes.toSeq, extensions)
    }

    for(job <- jobs)
      job.run()

  }

  private def loadMappingsFromCache(cache: File): Map[String, Int] = {
    val inputStream = new ObjectInputStream(new FileInputStream(cache))
    try
    {
      val map = inputStream.readObject().asInstanceOf[Map[String, Int]]

      logger.info(map.size + " mappings loaded from cache file "+cache)
      map
    }
    finally
    {
      inputStream.close()
    }
  }

  private def loadMappingsFromSource(language: Language, mappingFinder: DateFinder[File], mappings: Array[String], mappingSuffix: String): Map[String, Int] = {
    val map = new TrieMap[String, Int]

    // QuadReader iterates through the whole mappings file once for every language, reading in only the URIs for the current lang.
    for (mapping <- mappings) {
      var count = 0
      QuadReader.readQuads(mappingFinder, mapping + mappingSuffix, auto = true) {
        quad =>
          if (quad.datatype != null) throw new IllegalArgumentException(language.wikiCode + ": expected object uri, found object literal: " + quad)
          // TODO: this wastes a lot of space. Storing the part after ...dbpedia.org/resource/ would
          // be enough. Also, the fields of the Quad are derived by calling substring() on the whole
          // line, which means that the character array for the whole line is kept in memory, which
          // basically means that the whole redirects file is kept in memory. We should
          // - only store the resource title in the map
          // - use new String(quad.subject), new String(quad.value) to cut the link to the whole line
          // - maybe use an index of titles as in ProcessInterLanguageLinks to avoid storing duplicate titles

          //          // This checkLanguage manually extracts the host substring without creating a java.net.URL
          // Difference between performance: 9ms (for URL) per line vs 8ms per line.
          //          def checkLanguage(quadObject: String): Boolean = {
          //            val startDomain = quadObject.substring(7) // part after http://
          //            val endDomain = startDomain.indexOf("/")
          //            val domain = if(endDomain == -1) startDomain else startDomain.substring(0, endDomain)
          //            val languagePrefix = if(language.wikiCode == "en") "" else language.wikiCode + "."
          //            if(domain.startsWith(languagePrefix + "dbpedia.org")) true // only read current language mappings
          //            else if(domain.endsWith("dbpedia.org")) false
          //            else true // accept non-dbpedia URIs
          //          }

          def checkLanguage(quadObject: URL): Boolean = {
            val domain = quadObject.getHost
            val languagePrefix = if (language.wikiCode == "en") "" else language.wikiCode + "."
            if (domain.startsWith(languagePrefix + "dbpedia.org")) true // only read current language mappings
            else if (domain.endsWith("dbpedia.org")) false
            else true // accept non-dbpedia URIs
          }

          if (quad.datatype != null || checkLanguage(new URL(quad.value))) {
            map(quad.value) = quad.subject.substring(WikidataResource.length).toInt // Store just the Q-value integers
            count += 1
          }
      }

      logger.info(language.wikiCode + ": found " + count + " mappings")
    }

    map.toMap
  }

  class NormalizationJob(language: Language, destination: Destination, map: Map[String, Int], wikiFinder: Finder[File], inputs: Seq[String], suffixes: Seq[String], extensions: Seq[String]) {
    val outputExtension = extensions(0)
    val subjectRejectExtension = extensions(1)
    val objectRejectExtension = extensions(2)
    val bothRejectExtension = extensions(3)

    val workers = SimpleWorkers(1.5, 1.0) {
      args: (Quad, String) =>
        val (quad, input) = args
        (map.get(quad.subject), map.get(quad.value)) match {
          case (Some(subId), Some(objId)) =>
            destination.write(Seq(quad.copy(subject = WikidataResource + subId, value = WikidataResource + objId,
              dataset = input + outputExtension)))
          case (Some(id), _) if quad.datatype != null || !new URL(quad.value).getHost.contains("dbpedia.org") =>
            destination.write(Seq(quad.copy(subject = WikidataResource + id,
              dataset = input + outputExtension))) // Keep triples with string literals non-dbpedia URIs in object
          case (None, Some(id)) =>
            destination.write(Seq(quad.copy(dataset = input + subjectRejectExtension))) // Subject cannot be mapped in our URIs
          case (Some(id), None) =>
            destination.write(Seq(quad.copy(dataset = input + objectRejectExtension))) // Object does not exist
          case (None, None) =>
            destination.write(Seq(quad.copy(dataset = input + bothRejectExtension)))
          case _ =>
        }
    }

    def run(): Unit = {
      destination.open()
      workers.start()

      for (input <- inputs; suffix <- suffixes) {
        val date = wikiFinder.dates().last

        // Check for -correct version first, then -redirected, if not present, fall back to original name
        val inFile: FileLike[File] = {
          val correct = wrapFile(wikiFinder.file(date, input + "-correct" + suffix))
          if (correct.exists) {
            correct
          }
          else {
            val redirected = wrapFile(wikiFinder.file(date, input + "-redirected" + suffix))
            if (redirected.exists) {
              redirected
            } else {
              wrapFile(wikiFinder.file(date, input + suffix))
            }
          }
        }

        try {
          QuadReader.readQuads(wikiFinder.language.wikiCode+": Reading triples from " + input + suffix, inFile) {
            quad => workers.process((quad, input))
          }

        }
        catch {
          case e: Exception =>
            Console.err.println(e.printStackTrace())
        }
      }

      workers.stop()
      destination.close()
    }
  }

  private def createDestination[T <% FileLike[T]](finder: Finder[T], date: String, formats: scala.collection.Map[String, Formatter], datasets: Seq[Dataset]) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[String, Destination]()
      for (dataset <- datasets) {
        val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
        val sortedFile = finder.file(date, dataset.name.replace('_', '-')+"-sorted."+suffix)
        datasetDestinations(dataset.name) = new SortedDestination(new WriterDestination(writer(file), format), file, sortedFile)
      }

      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination.toSeq: _*)
  }

  private def writer[T <% FileLike[T]](file: T): () => Writer = {
    () => IOUtils.writer(file)
  }
}
