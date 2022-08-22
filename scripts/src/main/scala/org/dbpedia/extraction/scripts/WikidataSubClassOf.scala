package org.dbpedia.extraction.scripts

import java.io.{File, PrintWriter, StringWriter}
import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.config.{Config, ConfigUtils}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.mutable
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
  * needs to be run after the redirection of the wikidata-raw dataset
 */

object WikidataSubClassOf {

  val replaceStr = Language.Wikidata.resourceUri.namespace + "Q"
  val subClassProperty = "http://www.wikidata.org/entity/P279"


  def main(args: Array[String]) {

    require(args(0).nonEmpty, "missing required argument: config file name")

    val config = new Config(args(0))
    val suffix = config.inputSuffix match { case Some(s) => s}
    val rawDataset = DBpediaDatasets.WikidataRawRedirected.filenameEncoded + suffix

    val baseDir = config.dumpDir
    if (!baseDir.exists)
      throw new scala.IllegalArgumentException("dir " + baseDir + " does not exist")

    val finder = new DateFinder(baseDir, Language.Wikidata)
    finder.byName(rawDataset, auto = true)
    val ontology = new OntologyReader().read( XMLSource.fromFile(config.ontologyFile, Language.Mappings))

    // use integers in the map [superClass -> set[subclasses]]
    val wkdSubClassMap = getWikidataSubClassOfMap(rawDataset, finder)
    val wkdEquivMap: mutable.HashMap[Int, Option[String]] = new mutable.HashMap[Int, Option[String]]()

    // init all wikidata classes with None
    wkdSubClassMap.foreach( x => {

      if (!wkdEquivMap.contains(x._1)) wkdEquivMap.put(x._1, None)
      x._2.foreach( x2 => if (!wkdEquivMap.contains(x2)) wkdEquivMap.put(x2, None))
    })

    // init with DBpedia ontology
    ontology.wikidataClassesMap.foreach( c => {
      try {
        val wcID = c._1.replace("wikidata:Q", "").toInt
        c._2
          .filter(!_.name.contains(':'))
          .foreach( cls => wkdEquivMap.update(wcID, Some(cls.name)))
      }  catch {
        case e: NumberFormatException =>
          Console.err.println(e.printStackTrace())
      }

    })

    // we keep this as a reference to exclude the existing mappings at the end
    val existingMappings = wkdEquivMap.filter( x => x._2.nonEmpty).map(x => (x._1, x._2.get))

    Console.err.println("Ontology contains " + existingMappings.size + " class mappings")

    // Wikidata contains ontology cycles so we start with items high in the hierarchy
    startWithTopClasses(wkdSubClassMap, wkdEquivMap, ontology)

    // Do all mappings
    fillAllClasses(wkdSubClassMap, wkdEquivMap)



    //wkdToDbpMappings is a map[qID -> dbp Name
    val wkdToDbpMappings = wkdEquivMap
      .filter(_._2.isDefined)
      //exclude owl:Thing mappings
      .filter( x => !x._2.get.equals("owl:Thing"))
      // exclude existing mappings
      .filter( x => !existingMappings.contains(x._1))
      // remove the opional
      .map(x => ("Q"+x._1.toString(), x._2.get))

    writeConfig(config.wikidataMappingsFile, wkdToDbpMappings)

  }

  // Wikidata contains ontology cycles so we start with items high in the hierarchy
  private def startWithTopClasses(
    wkdSubClassMap: mutable.Map[Int, mutable.Set[Int]],
    wkdEquivMap: mutable.HashMap[Int, Option[String]], ontology: Ontology): Unit = {

    val wkdEntityQIDs = Set(35120) // Entity -> owl:Thing
    val owlThing = ontology.classes("owl:Thing")

    wkdEntityQIDs.foreach(qid => setWkdSubClassesToDbp(wkdEquivMap, wkdSubClassMap, qid, "owl:Thing"))
    Console.err.println("adding owl:Thing mappings, total :" + wkdEquivMap.filter(_._2.isDefined).size)


    // get top-level DBpedia classes
    val topLevelClasses = ontology.classes.values
        .filter( !_.name.contains(':'))
        .filter( _.baseClasses.contains(owlThing))
        .map(_.name)
        .toSet

    // perform this for all top-level classes
    wkdEquivMap.foreach( wd => {
      if (!wd._2.isEmpty && topLevelClasses.contains(wd._2.get)) {
        setWkdSubClassesToDbp(wkdEquivMap, wkdSubClassMap, wd._1, wd._2.get)
      }
    })
    Console.err.println("adding DBpedia top-level classes, total :" + wkdEquivMap.filter(_._2.isDefined).size)

    // should we add the 2nd level classes now?
  }

  private def fillAllClasses(
                                   wkdSubClassMap: mutable.Map[Int, mutable.Set[Int]],
                                   wkdEquivMap: mutable.HashMap[Int, Option[String]]): Unit = {


    var oldCount = wkdEquivMap.filter(_._2.isDefined).size
    var count = oldCount
    var pass = 0
    do {
      oldCount = count
      wkdEquivMap.foreach( wd => {
        if (!wd._2.isEmpty ) {
          setWkdSubClassesToDbp(wkdEquivMap, wkdSubClassMap, wd._1, wd._2.get)
        }
      })
      count = wkdEquivMap.filter(_._2.isDefined).size
      pass += 1
      Console.err.println("Pass #" + pass + ": total :" + count)
    } while (oldCount != count)


    count = wkdEquivMap.filter(_._2.isDefined).size

    // should we add the 2nd level classes now?
  }

  def getOntology(config: Properties): Ontology = {

    val ontologyFile = ConfigUtils.getValue(config, "ontology", false)(new File(_))
    val ontologySource = if (ontologyFile != null && ontologyFile.isFile) {
      XMLSource.fromFile(ontologyFile, Language.Mappings)
    }
    else {
      val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
      val url = new URL(Language.Mappings.apiUri)
      WikiSource.fromNamespaces(namespaces, url, Language.Mappings)
    }

    new OntologyReader().read(ontologySource)

  }

  def getWikidataSubClassOfMap(rawDataset: String, finder: DateFinder[File]): mutable.Map[Int, mutable.Set[Int]] = {
    val wikidataSubClassMap = mutable.Map.empty[Int, mutable.Set[Int]]
    try {
      new QuadMapper().readQuads(finder, rawDataset) { quad =>
        if (quad.predicate.equals(subClassProperty)) {

          try {
            val superClassID = quad.value.replace(replaceStr, "").toInt
            val subClassID = quad.subject.replace(replaceStr, "").toInt
            val subClassSet = wikidataSubClassMap.getOrElseUpdate(superClassID, new mutable.HashSet[Int]())
            subClassSet += subClassID

          } catch {
            case e: NumberFormatException =>
              //FIXME forward to extraction recorder
              Console.out.println(e.printStackTrace())
          }
        }
      }
      wikidataSubClassMap
    }
    catch {
      case e: Exception =>
        Console.err.println(e.printStackTrace())
        break
    }
  }

  private def writeConfig(outputFile: File, dbo_class_map: mutable.Map[String, String]): Unit = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    val json_out = new StringWriter

    mapper.writeValue(json_out, dbo_class_map)
    val json = json_out.toString()
    val pw = new PrintWriter(outputFile)
    pw.write(json)
    pw.close()
  }

  private def setWkdSubClassesToDbp(equivMap: mutable.HashMap[Int, Option[String]], subClsMap: mutable.Map[Int, mutable.Set[Int]]
                                    , wkdCls: Int, dbpCls: String) {
    subClsMap.get(wkdCls).foreach( s => {
      // exclude those that have a DBpedia type already
      s.filter(x => equivMap.contains(x) && equivMap.get(x).get.isEmpty)
      .foreach(y => {
        //set the type
        equivMap.update(y, Some(dbpCls))
        // run for subtypes transitively
        setWkdSubClassesToDbp(equivMap, subClsMap, y, dbpCls)
      })                          })
  }
}
