package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations.DestinationUtils
import org.dbpedia.extraction.destinations.formatters.{Formatter, TerseFormatter}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.concurrent
import scala.collection.convert.decorateAsScala._

/**
  * Created by Chile on 11/8/2016.
  */
object CanonicalizeIris {

  private val mappings = new ConcurrentHashMap[Language, concurrent.Map[String, String]]().asScala

  private val finders = new ConcurrentHashMap[Language, DateFinder[File]]().asScala

  private var baseDir: File = _

  private var genericLanguage: Language = Language.English
  private val dbpPrefix = "http://dbpedia.org"
  private var newLanguage: Language = _
  private var newPrefix: String = _
  private var newResource: String = _

  case class WorkParameters(language: Language, source: FileLike[File], destination: Dataset)

  private var frmts: Map[String, Formatter] = _

  /**
    * produces a copy of the Formatter map (necessary to guarantee concurrent processing)
 *
    * @return
    */
  def formats: Map[String, Formatter] = frmts.map(x => x._1 -> (x._2 match {
    case formatter: TerseFormatter => new QuadMapperFormatter(formatter)
    case _ => x._2.getClass.newInstance()
  }))

  def uriPrefix(language: Language): String = {
    val zw = if (language == genericLanguage)
      dbpPrefix
    else
      "http://" + language.dbpediaDomain
    zw+"/"
  }

  /**
    * get finder specific to a language directory
 *
    * @param lang
    * @return
    */
  def finder(lang: Language): DateFinder[File] = finders.get(lang) match{
    case Some(f) => f
    case None =>
      val f = new DateFinder(baseDir, lang)
      finders.put(lang, f)
      f.byName("download-complete", auto = true) //get date workaround
      f
  }

  /**
    * this worker reads the mapping file into a concurrent HashMap
    */
  val mappingsReader: (WorkParameters) => Unit = { langFile: WorkParameters =>
    val map = mappings.get(langFile.language) match {
      case Some(m) => m
      case None =>
        val zw = new ConcurrentHashMap[String, String]().asScala
        mappings.put(langFile.language, zw)
        zw
    }
    val oldReource = uriPrefix(langFile.language)+"resource/"
    val qReader = new QuadReader()
    qReader.readQuads(langFile.language, langFile.source) { quad =>
      if (quad.datatype != null)
        qReader.addQuadRecord(quad, langFile.language, null, new IllegalArgumentException("expected object uri, found object literal: " + quad))
      if (quad.value.startsWith(newResource)) {
        map(new String(quad.subject.replace(oldReource, ""))) = new String(quad.value.replace(newResource, ""))
      }
    }
  }

  /**
    * this worker does the actual mapping of URIs
    */
  val mappingExecutor: (WorkParameters) => Unit = { langSourceDest: WorkParameters =>
    val oldPrefix = uriPrefix(langSourceDest.language)
    val oldResource = oldPrefix+"resource/"

    val destination = DestinationUtils.createDatasetDestination(finder(langSourceDest.language), Seq(langSourceDest.destination), formats)
    val destName = langSourceDest.destination.encoded
    val map = mappings(langSourceDest.language) //if this fails something is really wrong

    def newUri(oldUri: String): String = {
      //let our properties pass :)
      //TODO maybe include a switch for this behavior?
      if(oldUri.startsWith(dbpPrefix + "ontology") || oldUri.startsWith(dbpPrefix + "property"))
        return oldUri
      if (oldUri.startsWith(oldPrefix))
        newPrefix + oldUri.substring(oldPrefix.length)
      else oldUri // not a DBpedia URI, copy it unchanged
    }

    def mapUri(oldUri: String): String = {
      if (oldUri.startsWith(oldResource))
        map.get(oldUri.replace(oldResource, "")) match{
          case Some(r) => newResource+r
          case None => null
        }
      else newUri(oldUri)
    }

    new QuadMapper().mapQuads(langSourceDest.language, langSourceDest.source, destination, required = false) { quad =>
      val pred = newUri(quad.predicate)
      val subj = mapUri(quad.subject)
      if (subj == null) {
        // no mapping for this subject URI - discard the quad. TODO: make this configurable
        List()
      }
      else if (quad.datatype == null && quad.language == null) {
        // URI value - change subject and object URIs, copy everything else
        val obj = mapUri(quad.value)
        if (obj == null) {
          // no mapping for this object URI - discard the quad. TODO: make this configurable
          List()
        } else {
          // map subject, predicate and object URI, copy everything else
          List(quad.copy(subject = subj, predicate = pred, value = obj, dataset = destName))
        }
      } else {
        // literal value - change subject and predicate URI, copy everything else
        List(quad.copy(subject = subj, predicate = pred, dataset = destName))
      }
    }
  }

  def main(args: Array[String]): Unit = {

    require(args != null && args.length == 1, "One arguments required, extraction properties file")

    val config = new Config(args.head)

    baseDir = config.dumpDir

    val mappings = config.getArbitraryStringProperty("mapping-files") match{
      case Some(x) => x.split(",").map(x => x.trim).distinct
      case None => Array()
    }
    require(mappings.nonEmpty, "Please provide a 'mapping-files' property in your properties configuration, defining the mapping dataset(s)")

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = config.getArbitraryStringProperty("mapping-suffix") match{
      case Some(x) => x.trim
      case None => throw new IllegalArgumentException("Please provide a 'mapping-suffix' in your properties configuration")
    }

    val inputs = config.inputDatasets
    require(inputs.nonEmpty, "Please provide a 'input' in your properties configuration, defining the input datasets")

    val inputSuffix = config.inputSuffix match{
      case Some(x) => x.trim
      case None => throw new IllegalArgumentException("Please provide a 'suffix' attribute in your properties configuration")
    }

    val extension = config.datasetnameExtension
    require(extension.nonEmpty, "Please provide a 'name-extension' in your properties configuration, defining the dataset name extension truncated at the end of the current dataset name")

    val threads = config.parallelProcesses

    // Language using generic domain (usually en)
    genericLanguage = config.getArbitraryStringProperty("generic-language") match{
      case Some(l) => Language(l.trim)
      case None => null
    }

    newLanguage = config.getArbitraryStringProperty("mapping-language") match{
      case Some(l) => Language(l)
      case None => throw new IllegalArgumentException("Please provide a 'mapping-language' in your properties configuration (usually 'en' or 'wikidata')")
    }
    newPrefix = uriPrefix(newLanguage)
    newResource = newPrefix+"resource/"

    val languages = config.languages
    require(languages.nonEmpty, "Please provide the 'languages' property in your properties configuration")

    frmts = config.formats.toMap

    // load all mappings
    val loadParameters = for (lang <- languages; mapping <- mappings)
      yield
        WorkParameters(language = lang, source = new RichFile(finder(lang).byName(mapping + mappingSuffix, auto = true).get), null)

    Workers.work[WorkParameters](SimpleWorkers(threads, threads)(mappingsReader), loadParameters.toList, "language mappings loading")

    //execute all file mappings
    val parameters = for (lang <- languages; input <- inputs)
      yield
        WorkParameters(language = lang, source = new RichFile(finder(lang).byName(input+inputSuffix, auto = true).get),
          DBpediaDatasets.getDataset(input+extension.getOrElse("")).getOrElse(
            throw new IllegalArgumentException("A dataset named " + input+extension.getOrElse("") + " is unknown.")
          ))

    Workers.work[WorkParameters](SimpleWorkers(threads, threads)(mappingExecutor), parameters.toList, "executing language mappings")
  }
}