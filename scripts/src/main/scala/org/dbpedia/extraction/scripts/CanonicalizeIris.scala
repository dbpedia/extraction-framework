package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.formatters.{Formatter, TerseFormatter}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, DestinationUtils}
import org.dbpedia.extraction.util.ConfigUtils._
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

  private var baseDir: File = null

  private var genericLanguage: Language = Language.English
  private val dbpPrefix = "http://dbpedia.org"
  private var newLanguage: Language = null
  private var newPrefix: String = null
  private var newResource: String = null

  private var formats: Map[String, Formatter] = null

  case class WorkParameters(language: Language, source: FileLike[_], destination: Dataset)

  def uriPrefix(language: Language): String = {
    val zw = if (language == genericLanguage)
      dbpPrefix
    else
      "http://" + language.dbpediaDomain
    zw+"/"
  }

  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }

  def finder(lang: Language) = finders.get(lang) match{
    case Some(f) => f
    case None => {
      val f = new DateFinder(baseDir, lang)
      finders.put(lang, f)
      f
    }
  }

  val mappingsReader = { langFile: WorkParameters =>
    val map = mappings.get(langFile.language) match {
      case Some(m) => m
      case None => {
        val zw = new ConcurrentHashMap[String, String]().asScala
        mappings.put(langFile.language, zw)
        zw
      }
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

  val mappingExecutor = { langSourceDest: WorkParameters =>

    val oldPrefix = uriPrefix(langSourceDest.language)
    val oldResource = oldPrefix+"resource/"

    val destination = DestinationUtils.createDestination(finder(langSourceDest.language), Seq(langSourceDest.destination), formats)
    val destName = langSourceDest.destination.name
    val map = mappings.get(langSourceDest.language).get //if this fails something is really wrong

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
      if (oldUri.startsWith(oldResource)) map.get(oldUri.replace(oldResource, "")) match{
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
      else if (quad.datatype == null) {
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

    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    baseDir = ConfigUtils.getValue(config, "base-dir", required=true)(new File(_))

    val mappings = ConfigUtils.getValues(config, "mapping-files",',', required=true)(x => x)
    require(mappings.nonEmpty, "no mapping datasets")

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = ConfigUtils.getString(config, "mapping-suffix",required=true)
    require(mappingSuffix.nonEmpty, "no mapping file suffix")

    val inputs = ConfigUtils.getValues(config, "input-files",',', required=true)(x => x)
    require(inputs.nonEmpty, "no input datasets")

    val extension = ConfigUtils.getString(config, "name-extension",required=true)
    require(extension.nonEmpty, "no result name extension")

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = ConfigUtils.getValues(config, "input-suffixes",',', required=true)(x => x)
    require(suffixes.nonEmpty, "no input/output file suffixes")

    val threads = Option(ConfigUtils.getValue[String](config, "parallel-threads",required=false)(x => x)) match{
      case Some(i) => Integer.parseInt(i)
      case None => 1
    }

    // Language using generic domain (usually en)
    genericLanguage = ConfigUtils.getValue(config, "generic-language",required=false)(x => Language(x))

    newLanguage = ConfigUtils.getValue(config, "mapping-language",required=true)(x => Language(x))
    newPrefix = uriPrefix(newLanguage)
    newResource = newPrefix+"resource/"

    val languages = parseLanguages(baseDir, ConfigUtils.getValues(config, "languages",',',required=true)(x => x))
    require(languages.nonEmpty, "no languages")

    formats = parseFormats(config, "uri-policy", "format").map( x=>
      x._1 -> (if(x._2.isInstanceOf[TerseFormatter]) new QuadMapperFormatter(x._2.asInstanceOf[TerseFormatter]) else x._2)).toMap

    // load all mappings
    val loadParameters = for (lang <- languages; mapping <- mappings)
      yield
        new WorkParameters(language = lang, source = new RichFile(finder(lang).byName(mapping+mappingSuffix, auto = true).get), null)

    Workers.work[WorkParameters](SimpleWorkers(threads, threads)(mappingsReader), loadParameters.toList, "language mappings loading")

    //execute all file mappings
    val parameters = for (lang <- languages; input <- inputs; suffix <- suffixes)
      yield
        new WorkParameters(language = lang, source = new RichFile(finder(lang).byName(input+suffix, auto = true).get), DBpediaDatasets.getDataset(input + extension).orNull)

    Workers.work[WorkParameters](SimpleWorkers(threads, threads)(mappingExecutor), parameters.toList, "executing language mappings")
  }
}