package org.dbpedia.extraction.scripts

import java.io.{PrintWriter, StringWriter, File, Writer}
import java.lang.annotation.Annotation
import java.net.URL

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{ConfigUtils, Finder, IOUtils, Language}
import org.dbpedia.extraction.wikiparser.Namespace
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.control.Breaks._

/**
 * Created by Ali Ismayilov
 *
 * Takes wikidata subclassof ttl file and generates new mappings from it.
 * For example:
 * Qx rdfs:subClassOf dbo:Class
 * Then creates new mapping
 * dbo:Class: {owl:equivalentClass: Qx}  and writes to file
 *
 */

object WikidataSubClassOf {

  def main(args: Array[String]) {

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val json_out = new StringWriter

    require(args != null && args.length == 2, "Two arguments required, extraction config file and extension to work with")
    require(args(0).nonEmpty, "missing required argument: config file name")
    require(args(1).nonEmpty, "missing required argument: suffix e.g. .tql.gz")

    val config = ConfigUtils.loadConfig(args(0), "UTF-8")
    val suffix = args(1)
    val subClassOfDataset = "ontology-subclassof." + suffix

    val baseDir = ConfigUtils.getValue(config, "base-dir", true)(new File(_))
    if (!baseDir.exists)
      throw new IllegalArgumentException("dir " + baseDir + " does not exist")
    val langConfString = ConfigUtils.getString(config, "languages", false)
    val languages = ConfigUtils.parseLanguages(baseDir, Seq(langConfString))
    val formats = parseFormats(config, "uri-policy", "format")

    for (lang <- languages) {

      // create destination for this language
      val finder = new Finder[File](baseDir, lang, "wiki")
      val date = finder.dates().last

      val resourceTypes = new scala.collection.mutable.HashMap[String, OntologyClass]()

      val dbo_class_map = mutable.Map.empty[String, mutable.Map[String, String]]
      try {
        QuadReader.readQuads(lang.wikiCode + ": Reading types from " + subClassOfDataset, finder.file(date, subClassOfDataset)) { quad =>
          val q = quad.copy(language = lang.wikiCode) //set the language of the Quad
        val class_key = q.value.replace("http://dbpedia.org/ontology/", "")

          val value = mutable.Map.empty[String, String]
          value += ("owl:equivalentClass" -> q.subject)
          dbo_class_map += (class_key -> value)
        }
      }
      catch {
        case e: Exception =>
          Console.err.println(e.printStackTrace())
          break
      }

      mapper.writeValue(json_out, dbo_class_map)
      val json = json_out.toString()
      val pw = new PrintWriter(new File("../dump/auto_generated_mapping.json"))
      pw.write(json)
      pw.close()
    }
  }
}
