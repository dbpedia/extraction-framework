package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.scripts.QuadMapper
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.datatypes.Datatype

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object PersonDataExtractorNew {
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    // load config
    val config = new Config(args(0))
    val baseDir = config.dumpDir
    if (!baseDir.exists) {
      throw error("dir " + baseDir + " does not exist")
    }
    val inputFinder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    val date = inputFinder.dates().last
    val output = config.outputDataset match{
      case Some (l) => l
      case None => throw new IllegalArgumentException("Please provide an 'output' attribute for the output dataset file in the .properties configuration.")
    }
    val languages = config.languages
    val formats = config.formats.toMap
    val input = config.inputDatasets.headOption.getOrElse(throw new IllegalArgumentException("Please provide an 'input' attribute for the wikidata input file in the .properties configuration."))

    val rawDataFile : RichFile = inputFinder.file(date, "raw.ttl.bz2").get

    val instanceFile : RichFile = inputFinder.file(date, "instance-types.ttl.bz2").get

    var files = Map[String, RichFile]()

    // additional input files for specific properties like label or description

    files += "$LABEL" -> inputFinder.file(date, "label.ttl.bz2").get

    files += "$ALIAS" -> inputFinder.file(date, "alias.ttl.bz2").get

    files += "$DESC"  -> inputFinder.file(date, "description.ttl.bz2").get


    val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("persondatamapping.json"))

    val extractor = new PersonDataExtractorNew(baseDir, files, instanceFile, rawDataFile, languages, formats, output, "<http://dbpedia.org/ontology/Person>", mappingsFile)
    extractor.extract()
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }

}

/**
  * Created by termi on 02.06.17.
  */
class PersonDataExtractorNew(baseDir : File, files : Map[String, RichFile], instanceFile : RichFile, rawDataFile : RichFile,
                              languages : Array[Language], formats : Map[String, Formatter],
                              output : String, instanceTypeOf: String, mappingsFile: JsonConfig) {

  // TODO: Destinations, could be DONE -> needs review

  private def setupDestinations(): Map[Language, Destination] = {
    var destinations = Map[Language, Destination]()
    languages.foreach( language => {
      val finder = new DateFinder[File](new Finder[File](baseDir,language, "wiki"))
      val dest : Destination = DestinationUtils.createDestination(finder, Array(DBpediaDatasets.Persondata), formats)
      destinations += language -> dest
    })
    destinations
  }

  def extract(): Unit = {
    val destinations = setupDestinations()
    val instanceMap = mutable.HashMap[String, mutable.HashMap[String, mutable.HashMap[String, String]]]()

    //Read Instance File
    new QuadMapper().readQuads(Language.Wikidata, instanceFile)(quad =>
      if(quad.value.equals(instanceTypeOf)){
        instanceMap.put(quad.subject, mutable.HashMap[String, mutable.HashMap[String, String]]())
      }
    )

    //----------------------------------------------------------------------------------------------------------------//
    // read additional files and gather data for our objects

    Workers.work(SimpleWorkers(1.5,1.5) { key : String =>
      files.get(key) match {
        case Some(file) =>
          new QuadMapper().readQuads(Language.Wikidata, file)(quad =>
            instanceMap.get(quad.subject) match {
              case Some(map) =>
                var value = quad.value
                if(value.length >= 3) {
                  if(value.trim.charAt(value.trim.length-3) == '@'){
                    // Get langcode from value and save it with the value in our map
                    var arr = value.trim.split("@").toList
                    val langcode::v  = arr.reverse
                    map.get(key) match {
                      case Some(langmap) => langmap += langcode.toString -> v.reverse.toString
                      case None => map += key -> mutable.HashMap(langcode.toString -> v.reverse.toString)
                    }
                  } else map.get(key) match {
                    case Some(langmap) => langmap += "other" -> value
                    case None => map += key -> mutable.HashMap("other" -> value)
                  }
                } else map.get(key) match {
                  case Some(langmap) => langmap += "other" -> value
                  case None => map += key -> mutable.HashMap("other" -> value)
                }
              case None =>
            })
        case None =>
      }
    }, files.keys.toList)

    //----------------------------------------------------------------------------------------------------------------//
    // read the raw data file and write the quads

    val literalMap = mappingsFile.getMap("literals")
    val objectMap = mappingsFile.getMap("objects")
    val multipleQuadsMap = mappingsFile.getMap("multiple-quads")

    // Worker Init
    val workers = SimpleWorkers(1.5,1.5) { job : JobEntity =>

      var new_quads = ArrayBuffer[Quad]()
      val subjectUri = job.quads.head.subject
      val wikicode = job.language.wikiCode

      job.quads.foreach(quad => {
        literalMap.get(quad.predicate) match{
          case Some(node) =>

            var value = quad.value
            var datatype = mappingsFile.getMap(quad.predicate)("datatype").asText()
            val propMap = mappingsFile.getMap(quad.predicate)("mapping").asText()

            //Special Cases
            if(datatype == "$DATE") datatype = parseDate(quad.value)

            //Properties gathered from other files
            files.keys.foreach(key => //which file?
              instanceMap(subjectUri).get(key) match {
                case Some(langmap) =>
                  langmap.get(wikicode) match { //which language
                    case Some(v) => value = v
                    case None =>
                      langmap.get("other") match { // no language specified in the data
                        case Some(v) => value = v
                        case None =>
                      }
                  }
                  // set the datatype
                  if(key == "$LABEL" || key == "$DESC" || key == "$ALIAS") datatype = "rdf:langString"
                  case None =>
              }
            )

            // quad building
            val new_quad = new Quad(job.language, DBpediaDatasets.Persondata, subjectUri, propMap, value, quad.context, new Datatype(datatype))
            new_quads += new_quad
          case None =>
        }

        objectMap.get(quad.predicate) match{
          case Some(node) =>
            val new_quad = new Quad(job.language, DBpediaDatasets.Persondata, subjectUri, objectMap(quad.predicate).asText(), quad.value, quad.context, null)
            new_quads += new_quad
          case None =>
        }
      })
      // write Quads
      destinations.get(job.language).foreach(_.write(new_quads))
    }

    // read raw file and process quads
    workers.start()
    new QuadMapper().readSortedQuads(Language.Wikidata, rawDataFile)(quads =>
    instanceMap.get(quads.head.subject) match {
      case Some(_) =>
        languages.foreach(language => {
          workers.process(new JobEntity(language, quads))
        })
      case None =>
    })
    workers.stop()
    destinations.foreach(_._2.close())
  }

  def parseDate(date : String) : String = {
    var format : String = ""
    val n = date.split("-").length
    if(n == 2) format = "yyyy-mm-dd"
    else if(n == 1) format = "yyyy-mm"
    else format = "yyyy"
    format
  }
  private class JobEntity(val language: Language, val quads : Traversable[Quad])
}
