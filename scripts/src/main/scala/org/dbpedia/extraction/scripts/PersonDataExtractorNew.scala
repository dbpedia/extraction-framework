package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer}

object PersonDataExtractorNew {
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    // load config
    val config = new Config(args(0))
    val baseDir = config.dumpDir
    if (!baseDir.exists) {
      throw error("dir " + baseDir + " does not exist")
    }

    val rawDataset = DBpediaDatasets.WikidataRawRedirected
    val suffix = config.inputSuffix match{
      case Some(suf) => suf
      case None => throw new IllegalArgumentException("no suffix option was provided in the properties file.")
    }

    val inputFinder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    val date = inputFinder.dates().last
    val dfinder = new DateFinder[File](inputFinder)

    //TODO after merge -> replace encoded with getfilename
    dfinder.byName(rawDataset.encoded.replace("_", "-") + suffix, auto = true) // work around for setting the date-finder date

    // output
    val destination = DestinationUtils.createDestination(dfinder,
      Array(DBpediaDatasets.Persondata.getLanguageVersion(Language.Wikidata, config.dbPediaVersion)), config.formats.toMap)

    // raw property data input
    val rawDataFile : RichFile = inputFinder.file(date, rawDataset.encoded.replace("_", "-") + suffix).get
    // file with the instance Type information
    val instanceFile : RichFile = inputFinder.file(date, DBpediaDatasets.OntologyTypes.encoded.replace("_", "-") + suffix).get
    // Mapping JSON
    val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("persondatamapping.json"))

    val ontology = {
      val ontologySource = ConfigUtils.getValue(Config.universalConfig.properties, "ontology")(new File(_))
      new OntologyReader().read( XMLSource.fromFile(ontologySource, Language.Mappings))
    }

    //TODO get rid of list when merged
    // Types of instances our subjects could be. Here: Subclasses of dbo:Person
    val instanceTypes = Array(
      "http://dbpedia.org/ontology/Person",
      "http://dbpedia.org/ontology/Actor",
      "http://dbpedia.org/ontology/Ambassador",
      "http://dbpedia.org/ontology/Archeologist",
      "http://dbpedia.org/ontology/Architect",
      "http://dbpedia.org/ontology/Aristocrat",
      "http://dbpedia.org/ontology/Artist",
      "http://dbpedia.org/ontology/Astronaut",
      "http://dbpedia.org/ontology/Athlete",
      "http://dbpedia.org/ontology/BeautyQueen",
      "http://dbpedia.org/ontology/BusinessPerson",
      "http://dbpedia.org/ontology/Celebrity",
      "http://dbpedia.org/ontology/Chef",
      "http://dbpedia.org/ontology/Cleric",
      "http://dbpedia.org/ontology/Coach",
      "http://dbpedia.org/ontology/Criminal",
      "http://dbpedia.org/ontology/Economist",
      "http://dbpedia.org/ontology/Egyptologist",
      "http://dbpedia.org/ontology/Engineer",
      "http://dbpedia.org/ontology/Farmer",
      "http://dbpedia.org/ontology/FictionalCharacter",
      "http://dbpedia.org/ontology/HorseTrainer",
      "http://dbpedia.org/ontology/Journalist",
      "http://dbpedia.org/ontology/Judge",
      "http://dbpedia.org/ontology/Lawyer",
      "http://dbpedia.org/ontology/Linguist",
      "http://dbpedia.org/ontology/MemberResistanceMovement",
      "http://dbpedia.org/ontology/MilitaryPerson",
      "http://dbpedia.org/ontology/Model",
      "http://dbpedia.org/ontology/Monarch",
      "http://dbpedia.org/ontology/MovieDirector",
      "http://dbpedia.org/ontology/Noble",
      "http://dbpedia.org/ontology/OfficeHolder",
      "http://dbpedia.org/ontology/OrganisationMember",
      "http://dbpedia.org/ontology/Orphan",
      "http://dbpedia.org/ontology/Philosopher",
      "http://dbpedia.org/ontology/PlayboyPlaymate",
      "http://dbpedia.org/ontology/Politician",
      "http://dbpedia.org/ontology/PoliticianSpouse",
      "http://dbpedia.org/ontology/Presenter",
      "http://dbpedia.org/ontology/Producer",
      "http://dbpedia.org/ontology/Psychologist",
      "http://dbpedia.org/ontology/Referee",
      "http://dbpedia.org/ontology/Religious",
      "http://dbpedia.org/ontology/RomanEmperor",
      "http://dbpedia.org/ontology/Royalty",
      "http://dbpedia.org/ontology/Scientist",
      "http://dbpedia.org/ontology/SportsManager",
      "http://dbpedia.org/ontology/TelevisionDirector",
      "http://dbpedia.org/ontology/TelevisionPersonality",
      "http://dbpedia.org/ontology/TheatreDirector",
      "http://dbpedia.org/ontology/Writer")

    val extractor = new PersonDataExtractorNew(baseDir, instanceFile, rawDataFile, destination, instanceTypes, mappingsFile)
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
  */
class PersonDataExtractorNew(baseDir : File, instanceFile : RichFile, rawDataFile : RichFile,
                              destination: Destination, instanceTypeOf: Array[String], mappingsFile: JsonConfig) {

  def extract(): Unit = {

    val instanceMap = mutable.HashMap[String, String]()

    //Read Instance File
    new QuadMapper().readQuads(Language.Wikidata, instanceFile)(quad =>
      if(instanceTypeOf.contains(quad.value)){
        instanceMap.put(quad.subject, null)
      }
    )

    // read raw file and process quads
    destination.open()
    new QuadMapper().readSortedQuads(Language.Wikidata, rawDataFile)(quads => {
      var new_quads = Traversable[Quad]()
      if(quads.nonEmpty) {
        instanceMap.get(quads.head.subject) match {
          case Some(o) =>
            // Subject is an instance of our desired Type or Subtype of it
            new_quads = process(quads)
          case None =>
            // Subject is not defined as Person => check if the properties are maybe similar
            var similarity = 0
            quads.foreach(quad => {
              if(mappingsFile.keys().toList.contains(quad.predicate))
                similarity += 1
            })
            if(similarity >= 3)
              new_quads = process(quads)
        }
      }
      destination.write(new_quads)
    })
    destination.close()
  }

  def maptoLanguage(languages: Traversable[Language]): Unit ={
    // TODO
    // read sameAs File of Language
    // read our new PersonData File
    // replace subjects and objects
    // save in Language Folder
  }

  def process(quads : Traversable[Quad]) : Traversable[Quad] =  {
    var new_quads = ArrayBuffer[Quad]()
    quads.foreach(quad => {
      val subject = new String(quad.subject)
      val value = new String(quad.value)

      mappingsFile.get(quad.predicate) match{
        case Some(node) =>
          // datatype is either defined by the quad (value is literal) or null (value is another resource)
          var datatype : Datatype = null
          if(quad.datatype != null)
            datatype = new Datatype(quad.datatype)
          new_quads += new Quad(
            Language.Wikidata,
            DBpediaDatasets.Persondata,
            subject,
            node.asText(),
            value,
            if(quad.context != null) quad.context else null,
            datatype)
        case None =>
      }
    })
    new_quads
  }
}
