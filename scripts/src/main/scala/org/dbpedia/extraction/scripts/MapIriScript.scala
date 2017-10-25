package org.dbpedia.extraction.scripts

import java.io.File

import org.apache.jena.ext.com.google.common.collect.{Multimaps, SortedSetMultimap, TreeMultimap}
import org.dbpedia.extraction.config.{Config, ConfigUtils}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.iri.{IRI, URI, UriUtils}

import scala.collection.convert.decorateAsScala._
import scala.Console.err

/**
  * Created by chile on 17.10.17.
  */
object MapIriScript {


  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 1, "This script needs a single argument - the properties file config")

    val config = new Config(args.head)
    val baseDir = config.dumpDir

    // Suffix of mapping files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val mappingSuffix = config.getArbitraryStringProperty("mappings-suffix").getOrElse("")

    //any NTriple file
    val mappingDatasets = ConfigUtils.getStrings(config, "mapping-files", ",", required = true)
    require(mappingDatasets.nonEmpty, "no mapping datasets")

    val filterOutNonMatches = config.getArbitraryStringProperty("filter-out-non-matches").getOrElse("false").toBoolean

    val toIri = config.getArbitraryStringProperty("convert-to-iris").getOrElse("false").toBoolean

    val nameExtension = config.getArbitraryStringProperty("file-name-extension").getOrElse("-redirected")

    val mapToObject = config.getArbitraryStringProperty("mapping-direction") match {
      case Some(s)
        if s.trim == "subject->object" || s.trim == "object->subject" => if ("subject->object" == s.trim) true else false
      case Some(d) => throw new IllegalArgumentException("The property mapping-direction must either be \"subject->object\" or \"object->subject\"!")
      case None => true
    }

    val replacementPredicate =config.getArbitraryStringProperty("replacement-predicate") match{
      case Some(f) if f.trim.nonEmpty && IRI.create(f).isSuccess => Some(f)
      case _ => None
    }

    val mappingFilter=config.getArbitraryStringProperty("mappings-filter") match{
      case Some(f) if f.trim.nonEmpty => Some(f.split(",").head, f.split(",")(1))
      case _ => None
    }

    require(config.inputDatasets.nonEmpty, "Please provide a list of input datasets!")

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val inputs = config.inputSuffix match{
      case Some(s) if s.trim.nonEmpty => config.inputDatasets.map(x => x -> s)
      case _ => config.inputDatasets.map(x => {
          x.split("\\.").head -> ("." + x.split("\\.").tail.reduce((a,b) => a + "." + b))
      })
    }

    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val isExternal = baseDir.listFiles().count(f => f.isDirectory && f.toString.endsWith(config.wikiName)) == 0

    val languages = if (isExternal) Array(Language.English) else config.languages
    require(languages.nonEmpty, "no languages")

    val mappingTarget = config.getArbitraryStringProperty("mapping-target", required = true) match {
      case x if x == "subject" || x == "predicate" || x == "object" => x
      case _ => throw new IllegalArgumentException("The property mapping-target must be \"subject\" | \"predicate\" | \"object\" - no other value allowed!")
    }

    def resolveTarget(quad: Quad, target: String): String = target match {
      case "subject" => quad.subject
      case "predicate" => quad.predicate
      case "object" => quad.value
      case _ => throw new IllegalArgumentException("The property mapping-target must be \"subject\" | \"predicate\" | \"object\" - no other value allowed!")
    }

    def recodeQuad(quad: Quad, map: SortedSetMultimap[String, String]): Seq[Quad] = {
      var changed = false
      var subj = quad.subject
      var pred = quad.predicate
      var obj = quad.value
      var cont = quad.context
      //if we want to change to Iri
      if(toIri) {
        subj = UriUtils.uriToDbpediaIri(quad.subject).toString
        changed = changed || subj != quad.subject
        pred = UriUtils.uriToDbpediaIri(quad.predicate).toString
        changed = changed || pred != quad.predicate
        obj = if (quad.datatype == null) UriUtils.uriToDbpediaIri(quad.value).toString else quad.value
        changed = changed || obj != quad.value
        cont = if (quad.context != null) UriUtils.uriToDbpediaIri(quad.context).toString else quad.context
        changed = changed || cont != quad.context
      }
      //replace pred if needed
      replacementPredicate match{
        case Some(p) => pred = p
        case None =>
      }
      val copy = if(changed) quad.copy(subject = subj, predicate = pred, value = obj, context = cont) else quad

      val uris = map.get(resolveTarget(copy, mappingTarget)).asScala
      val ret = for (uri <- uris; enc = UriUtils.uriToDbpediaIri(uri).toString)
        yield mappingTarget match {
          case "subject" => copy.copy(subject = enc, context = if (quad.context == null) quad.context else quad.context + "&" + mappingTarget + "MappedFrom=" + quad.subject)
          case "predicate" => copy.copy(predicate = enc, context = if (quad.context == null) quad.context else quad.context + "&" + mappingTarget + "MappedFrom=" + quad.predicate)
          case "object" => copy.copy(value = enc, context = if (quad.context == null) quad.context else quad.context + "&" + mappingTarget + "MappedFrom=" + quad.value)
          case _ => throw new IllegalArgumentException("The property mapping-target must be \"subject\" | \"predicate\" | \"object\" - no other value allowed!")
        }
      if (ret.isEmpty && !filterOutNonMatches)
        List(copy)
      else
        ret.toSeq
    }

    for (language <- languages) {
      val finder = new DateFinder[File](baseDir, language)
      val map = Multimaps.synchronizedSortedSetMultimap[String, String](TreeMultimap.create[String, String]())

      Workers.work(SimpleWorkers(1.5, 1.0) { mapping: String =>
        var count = 0
        val file = if (isExternal) {
          if (URI.create(mapping).isSuccess) //is absolute
            ConfigUtils.loadRichFile(mapping + mappingSuffix, exists = true)
          else
            ConfigUtils.loadRichFile(baseDir.getFile.toString + "/" + mapping + mappingSuffix, exists = true)
        }
        else
          finder.byName(mapping + mappingSuffix, auto = true) match {
            case Some(f) => wrapFile(f)
            case None => throw new IllegalArgumentException("No file found for language: " + language + " : " + mapping + mappingSuffix)
          }
        new QuadMapper().readQuads(language, file) { quad =>
          mappingFilter match{
            case Some(f) if ! resolveTarget(quad, f._1).startsWith(f._2) => // if there is a filter and it matches not the current quad -> ignore
            case _ =>                                                       // if there is no filter or it matches the current quad
              if (quad.datatype != null) throw new IllegalArgumentException(mapping + ": expected object uri, found object literal: " + quad)
              if (mapToObject)
                map.put(quad.subject, quad.value)
              else
                map.put(quad.value, quad.subject)
              count += 1
          }
        }
        err.println(mapping + ": found " + count + " mappings")
      }, mappingDatasets.toList)

      Workers.work(SimpleWorkers(1.5, 1.0) { input: (String, String) =>
        var count = 0
        val outputFile = if (isExternal)
          ConfigUtils.loadRichFile(baseDir.toString + "/" + input._1 + nameExtension + input._2, exists = false)
        else
          finder.byName(input._1 + nameExtension + input._2, auto = true) match {
            case Some(f) => wrapFile(f)
            case None => throw new IllegalArgumentException("No file found for language: " + language + " : " + input._1 + input._2)
          }
        val inputFile = if (isExternal)
          ConfigUtils.loadRichFile(baseDir.toString + "/" + input._1 + input._2, exists = true)
        else
          finder.byName(input._1 + input._2, auto = true) match {
            case Some(f) => wrapFile(f)
            case None => throw new IllegalArgumentException("No file found for language: " + language + " : " + input._1 + input._2)
          }

        new QuadMapper().mapQuads(language, inputFile, outputFile) { quad =>
          val quads = recodeQuad(quad, map)
          if(quads.nonEmpty && quads.head != quad)
            count = count + 1
          quads
        }
        err.println(input._1 + ": changed " + count + " quads.")
      }, inputs.toList)
    }
  }
}
