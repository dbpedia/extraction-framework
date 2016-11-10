package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.scripts.QuadMapper.QuadMapperFormatter
import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.util.IOUtils._
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile

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

  private val formatter = new QuadMapperFormatter()

  case class WorkParameters(language: Language, source: FileLike[_], destination: WriterDestination)

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

  val mappingsReader = SimpleWorkers(1.5, 1.0) { langFile: WorkParameters =>
    val map = mappings.get(langFile.language) match {
      case Some(m) => m
      case None => {
        val zw = new ConcurrentHashMap[String, String]().asScala
        mappings.put(langFile.language, zw)
        zw
      }
    }
    val oldReource = uriPrefix(langFile.language)+"resource/"
    QuadReader.readQuads(langFile.language, langFile.source) { quad =>
      if (quad.datatype != null)
        QuadReader.addQuadRecord(quad, langFile.language, null, new IllegalArgumentException("expected object uri, found object literal: " + quad))
      if (quad.value.startsWith(newResource)) {
        map(new String(quad.subject.replace(oldReource, ""))) = new String(quad.value.replace(newResource, ""))
      }
    }
  }

  val mappingExecutor = SimpleWorkers(1.5, 1.0) { langSourceDest: WorkParameters =>

    val oldPrefix = uriPrefix(langSourceDest.language)
    val oldResource = oldPrefix+"resource/"

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

    QuadMapper.mapQuads(langSourceDest.language, langSourceDest.source, langSourceDest.destination, required = false) { quad =>
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

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 9,
      "need at least nine args: " +
        /*0*/ "base dir, " +
        /*1*/ "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), " +
        /*2*/ "mapping file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
        /*3*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), " +
        /*4*/ "output dataset name extension (e.g. '-en-uris'), " +
        /*5*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
        /*6*/ "wiki code of generic domain (e.g. 'en', use '-' to disable), " +
        /*7*/ "wiki code of new URIs (e.g. 'en'), " +
        /*8*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")

    baseDir = new File(args(0))

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
    genericLanguage = if (args(6) == "-") null else Language(args(6))

    newLanguage = Language(args(7))
    newPrefix = uriPrefix(newLanguage)
    newResource = newPrefix+"resource/"

    val languages = parseLanguages(baseDir, args.drop(8))
    require(languages.nonEmpty, "no languages")

    // load all mappings
    val loadParameters = for (lang <- languages; mapping <- mappings)
      yield
        new WorkParameters(language = lang, source = new RichFile(finder(lang).byName(mapping+mappingSuffix, auto = true).get), null)

    Workers.work[WorkParameters](mappingsReader, loadParameters.toList, "language mappings loading")

    val parameters = for (lang <- languages; input <- inputs; suffix <- suffixes)
      yield
        new WorkParameters(language = lang, source = new RichFile(finder(lang).byName(input+suffix, auto = true).get),
          new WriterDestination(() => writer(finder(lang).byName(input + extension + suffix, auto = false).get), formatter))

    Workers.work[WorkParameters](mappingExecutor, parameters.toList)
  }
}