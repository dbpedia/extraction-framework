package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.ontology.{OntologyClass, RdfNamespace}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object PersonDataExtractorNew {
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    // load config
    val config = new Config(args(0))
    val baseDir = config.dumpDir
    if (!baseDir.exists) {
      throw error("dir " + baseDir + " does not exist")
    }
    //start the extractor
    val extractor = new PersonDataExtractorNew(config: Config)
    extractor.extract()
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
}

/**
  * Created by Robert Bielinski on 02.06.17.
  * PersonData Extractor for the new Wikipedia Template standards.
  * Runs on wikidata raw file, outputs the wikidata PersonData file.
  *
  * Input Files need to be sorted!
  */
class PersonDataExtractorNew(config: Config) {

  //TODO make sorted datasets mandatory

  private val suffix = config.inputSuffix match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("input-suffix parameter was not provided in the properties file")
  }

  private val ontology = {
    val ontologySource = config.ontologyFile
    new OntologyReader().read( XMLSource.fromFile(ontologySource, Language.Mappings))
  }

  //get dbo:Person
  private val person = ontology.getOntologyClass("Person") match{
    case Some(p) => p
    case None => throw new IllegalArgumentException("Class dbo:Person was not found!")
  }

  //collect all sub-classes from dbo:Person
  private val personTypes = ontology.classes.values.filter(x => x.isSubclassOf(person)).map(x => x.uri).toList


  private val dfinder = new DateFinder(config.dumpDir, Language.Wikidata)

  dfinder.byName(DBpediaDatasets.WikidataRawRedirected.filenameEncoded + suffix, auto = true) // work around for setting the date-finder date

  // output
  private val finalDestination: Destination = DestinationUtils.createDatasetDestination(dfinder,
    List(DBpediaDatasets.Persondata.getLanguageVersion(Language.Wikidata, config.dbPediaVersion).get),
    config.formats.toMap)

  private val testination: Destination = DestinationUtils.createDatasetDestination(dfinder,
    List(DBpediaDatasets.WikidataPersondataRaw.getLanguageVersion(Language.Wikidata, config.dbPediaVersion).get),
    config.formats.toMap)

  private val testSource = dfinder.byName(DBpediaDatasets.WikidataPersondataRaw.filenameEncoded + suffix, auto = true) match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Wikidata input file was not found: " + DBpediaDatasets.WikidataPersondataRaw.filenameEncoded)
  }

  // raw property data input
  private val rawDataFile : RichFile = dfinder.byName(DBpediaDatasets.WikidataRawRedirected.filenameEncoded + "-sorted" + suffix, auto = true) match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Wikidata input file was not found: " + DBpediaDatasets.WikidataRawRedirected.filenameEncoded)
  }
  // file with the instance Type information
  private val instanceFile : RichFile =  dfinder.byName(DBpediaDatasets.OntologyTypes.filenameEncoded + suffix, auto = true) match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Wikidata input file was not found: " + DBpediaDatasets.OntologyTypes.filenameEncoded)
  }
  // raw property data input
  private val labelsFile : RichFile = dfinder.byName(DBpediaDatasets.Labels.filenameEncoded + "-sorted" + suffix, auto = true) match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Wikidata input file was not found: " + DBpediaDatasets.WikidataRawRedirected.filenameEncoded)
  }
  // raw property data input
  private val descriptionFile : RichFile = dfinder.byName(DBpediaDatasets.WikidataDescriptionMappingsWiki.filenameEncoded + "-sorted" + suffix, auto = true) match{
    case Some(x) => x
    case None => throw new IllegalArgumentException("Wikidata input file was not found: " + DBpediaDatasets.WikidataRawRedirected.filenameEncoded)
  }
  
  private val rdfslabel = RdfNamespace.resolvePrefix("rdfs:label")

  // Mapping JSON
  private val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("persondatamapping.json"))

  private var mappingValues = new mutable.HashMap[(String, String), ListBuffer[(String, String)]]()

  private var propertyPathMap = mappingsFile.configMap.map(x => {
    val zw = new ListBuffer[String]()
    for(i <- 0 until x._2.size)
        zw.append(RdfNamespace.resolvePrefix(x._2.get(i).asText()))
    zw.append(RdfNamespace.resolvePrefix(x._1))
    zw.head -> zw.tail.toList
  })

  private def updatePropertyMap = {
    propertyPathMap = propertyPathMap.map(x =>
      if(x._2.nonEmpty && RdfNamespace.resolvePrefix(x._2.head) != rdfslabel)
        x._2.head -> x._2.tail
      else
        x._1 -> x._2
    )
  }

  def extract(): Unit = {

    val instanceMap = mutable.HashMap[String, String]()

    //Read Instance File
    new QuadMapper().readQuads(Language.Wikidata, instanceFile)(quad =>
      if (personTypes.contains(quad.value)) {
        instanceMap.put(quad.subject, quad.value)
      }
    )

    // read raw file and process quads
    new QuadMapper().mapSortedQuads(Language.Wikidata, rawDataFile, testination, required = true) { quads =>
      var new_quads = ArrayBuffer[Quad]()
      if (quads.nonEmpty) {
        instanceMap.get(quads.head.subject) match {
          case Some(o) =>
            // Subject is an instance of our desired Type or Subtype of it
            new_quads ++= process(quads)
          case None =>
            // Subject is not defined as Person => check if the properties are maybe similar
            val similarity = quads.count(q => propertyPathMap.keySet.toList.contains(q.predicate))
            if (similarity >= 2) {
              new_quads ++= process(quads)
              instanceMap.put(quads.head.subject, person.uri)
            }
        }
        instanceMap.get(quads.head.subject) match {
          case Some(typp) => new_quads += quads.head.copy(
            dataset = DBpediaDatasets.WikidataPersondataRaw.encoded,
            predicate = RdfNamespace.resolvePrefix("rdf:type"),
            datatype = null,
            value = typp)
          case None =>
        }
      }
      new_quads
    }
    updatePropertyMap

    while (!propertyPathMap.values.forall(x => x.isEmpty || RdfNamespace.resolvePrefix(x.head) == rdfslabel)) {
      new QuadMapper().readSortedQuads(Language.Wikidata, rawDataFile) { quads =>
        if (quads.nonEmpty)
          process2(quads)
      }
      updatePropertyMap
    }

    val finalValueMap = mappingValues.filter(x => x._1._2 != "http://www.w3.org/2000/01/rdf-schema#label").flatMap(x => x._2.map(y => y -> x._1))

    //FIXME the last three mappings need to be transformed into a readSortedQuads of multiple files (test, labels, description) after merge with quad utility
    new QuadMapper().mapSortedQuads(Language.Wikidata, testSource, finalDestination, required = true, closeWriter = false) { quads =>
      var new_quads = new ArrayBuffer[Quad]()
      if (quads.nonEmpty) {
        for (quad <- quads) {
          val value = finalValueMap.get((quad.subject, quad.predicate)) match {
            case Some(v) => v._1
            case None => quad.value
          }
          new_quads += quad.copy(value = value, dataset = DBpediaDatasets.Persondata.encoded)
        }
      }
      new_quads
    }

    new QuadMapper().mapSortedQuads(Language.Wikidata, labelsFile, finalDestination, required = true, closeWriter = false) { quads =>
      var new_quads = new ArrayBuffer[Quad]()
      if (quads.nonEmpty) {
        mappingValues.get((quads.head.subject, rdfslabel)) match {
          case Some(q) => q.foreach(z => new_quads ++= quads.map(x => x.copy(subject = z._1, predicate = z._2, dataset = DBpediaDatasets.Persondata.encoded)))
          case None =>
        }
        instanceMap.get(quads.head.subject) match {
          case Some(p) => new_quads ++= quads.map(x => x.copy(predicate = RdfNamespace.resolvePrefix("foaf:name"), dataset = DBpediaDatasets.Persondata.encoded))
          case None =>
        }
      }
      new_quads
    }

    new QuadMapper().mapSortedQuads(Language.Wikidata, descriptionFile, finalDestination, required = true) { quads =>
      if (quads.nonEmpty) {
        instanceMap.get(quads.head.subject) match {
          case Some(p) => quads.map(x => x.copy(predicate = RdfNamespace.resolvePrefix("dct:description"), dataset = DBpediaDatasets.Persondata.encoded))
          case None => Seq()
        }
      }
      else
        Seq()
    }
  }

  def process(quads : Traversable[Quad]) : Traversable[Quad] =  {
    var new_quads = ArrayBuffer[Quad]()
    quads.foreach(quad => {
      propertyPathMap.get(quad.predicate) match{
        case Some(properties) =>
          if(properties.size > 1){
            mappingValues.get((quad.value, properties.head)) match{
              case Some(h) => h.append((quad.subject, properties.reverse.head))
              case None => {
                val zw = new ListBuffer[(String, String)]()
                zw.append((quad.subject, properties.reverse.head))
                mappingValues.put((quad.value, properties.head), zw)
              }
            }
          }
          else
            // datatype is either defined by the quad (value is literal) or null (value is another resource)
            new_quads += new Quad(
              Language.Wikidata,
              DBpediaDatasets.WikidataPersondataRaw,
              quad.subject,
              properties.reverse.head,
              quad.value,
              if(quad.context != null)
                quad.context
              else null,
              if(quad.datatype != null)
                new Datatype(quad.datatype)
              else null)
          case None =>
      }
    })
    new_quads
  }

  def process2(quads : Traversable[Quad]) :Unit =  {
    quads.foreach(quad => {

      mappingValues.get((quad.subject, quad.predicate)) match{
        case Some(origSubj) => {
          val properties = propertyPathMap(quad.predicate)
          if (properties.nonEmpty)
            mappingValues.put((quad.value, RdfNamespace.resolvePrefix(properties.head)), origSubj)
          else
            mappingValues.put((quad.value, null), origSubj)

          mappingValues.remove((quad.subject, quad.predicate))
        }
        case None =>
      }
    })
  }
}
