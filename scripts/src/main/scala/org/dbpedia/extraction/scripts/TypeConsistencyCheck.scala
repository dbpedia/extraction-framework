package org.dbpedia.extraction.scripts

import java.io.{File, Writer}
import java.net.URL

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

import TypeConsistencyCheck._

/**
 * Created by Markus Freudenberg
 *
 * Takes the mapping-based properties dataset and the assigned rdf types and tries
 * to classify them in correct or wrong statements.
 *
 * Wrong statements are when the type of object IRI is disjoint with the property definition
 * For correct we have different types but we skip them for now
 * 1) Correct type/subtype
 * 2) not correct type/subtype but not disjoint
 * 3) the object IRI is untyped
 * all 1-3 are for now kept together and not split
 *
 * TODO: this needs special care for English where nt/ttl use different IRIs/URIs
 */
object TypeConsistencyCheck {

  /**
   * different datasets where we store the mapped triples depending on their state
   */
  val correctDataset: Dataset = DBpediaDatasets.OntologyPropertiesObjectsCleaned
  //range based datasets
  val disjointRangeDataset: Dataset  = DBpediaDatasets.OntologyPropertiesDisjointRange
  val untypedRangeDataset: Dataset = correctDataset //new Dataset("mappingbased-properties-untyped");
  val nonDisjointRangeDataset: Dataset = correctDataset //new Dataset("mappingbased-properties-non-disjoint");

  //domain based datasets
  val disjointDomainDataset: Dataset  = DBpediaDatasets.OntologyPropertiesDisjointDomain
  val untypedDomainDataset: Dataset = correctDataset //new Dataset("mappingbased-properties-untyped");
  val nonDisjointDomainDataset: Dataset = correctDataset //new Dataset("mappingbased-properties-non-disjoint");

  val datasets: Seq[Dataset] = Seq(correctDataset, disjointRangeDataset, disjointDomainDataset)

  def main(args: Array[String]) {


    require(args != null && args.length == 1, "Two arguments required, extraction config file and extension to work with")
    require(args(0).nonEmpty, "missing required argument: config file name")
    //require(args(1).nonEmpty, "missing required argument: suffix e.g. .tql.gz")


    val config = new Config(args(0))

    val baseDir = config.dumpDir
    if (!baseDir.exists)
      throw new IllegalArgumentException("dir " + baseDir + " does not exist")

    val languages = config.languages

    val formats = config.formats

    lazy val ontology: Ontology = {
      val ontologyFile = config.ontologyFile
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

    val suffix = config.inputSuffix match {
      case Some(x) => x
      case None => throw new IllegalArgumentException("Please provide a 'suffix' attribute in your properties configuration")
    }
    val typesDataset = "instance-types" + suffix
    val mappedTripleDataset = "mappingbased-objects-uncleaned" + suffix


    for (lang <- languages) {

      // create destination for this language
      val finder = new Finder[File](baseDir, lang, "wiki")
      val date = finder.dates().last
      val destination = createDestination(finder, date, formats)

      val typeDatasetFile: File = finder.file(date, typesDataset).get
      val mappedTripleDatasetFile: File = finder.file(date, mappedTripleDataset).get
      checkTypeConsistency(ontology, typeDatasetFile, mappedTripleDatasetFile, destination, lang)
    }
  }


  def checkTypeConsistency(ontology: Ontology, typesDatasetFile: File, mappedTripleDatasetFile: File, destination: Destination, lang: Language = Language.English): Unit = {

    val resourceTypes = new scala.collection.mutable.HashMap[String, OntologyClass]()


    try {
      new QuadMapper().readQuads(lang, typesDatasetFile) { quad =>
        val q = quad.copy(language = lang.wikiCode) //set the language of the Quad
        computeType(quad, resourceTypes, ontology)
      }
    }
    catch {
      case e: Exception =>
        Console.err.println(e.printStackTrace())
        break
    }


    try {
      destination.open()
      new QuadMapper().readQuads(lang, mappedTripleDatasetFile) { quad =>

        val rangeDataset = checkQuadRange(quad, resourceTypes, ontology)
        val domainDataset = checkQuadDomain(quad, resourceTypes, ontology)

        val datasetList = List(rangeDataset.encoded, domainDataset.encoded).distinct
        val verifiedDatasets: List[String] = {
          if (datasetList.size == 1 && datasetList.contains(correctDataset.encoded)) {
            datasetList
          }
          else {
            datasetList.filter(_ != correctDataset.encoded)
          }
        }
        for (d <- verifiedDatasets) {
          val q = quad.copy(language = lang.wikiCode, dataset = d) //set the language of the Quad
          try {
            destination.write(Seq(q))
          } catch {
            case e: Exception => Console.err.println(e.printStackTrace())
          }
        }
      }
      destination.close()
    }
    catch {
      case e: Exception =>
        Console.err.println(e.printStackTrace())
        break
    }
  }

  /**
    * Checks a Quad for range violations and returns the new dataset where it should be written depending on the state
    * @return
    */
  def checkQuadRange(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Dataset =
  {
    //if datatype property we carry on
    if (quad.datatype != null)
      return correctDataset

    resourceTypes.get(quad.value) match {
      case Some(obj) => ontology.getOntologyProperty(quad.predicate) match{
        case Some(predicate) if predicate.range.equals(OntologyClass.owlThing) => correctDataset
        case Some(predicate) if obj.relatedClasses.contains(predicate.range) => correctDataset
        case Some(predicate) if predicate.range.isInstanceOf[OntologyClass] && isDisjoined(obj, predicate.range.asInstanceOf[OntologyClass]) => disjointRangeDataset
        case Some(predicate) => nonDisjointRangeDataset
        case None => untypedRangeDataset
      }
      case None => untypedRangeDataset
    }
  }

  /**
    * Checks a Quad for domain violations and returns the new dataset where it should be written depending on the state
    * @return
    */
  def checkQuadDomain(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Dataset =
  {
    //object is uri
    resourceTypes.get(quad.subject) match {
      case Some(subj) => ontology.getOntologyProperty(quad.predicate) match{
        case Some(predicate) if predicate.domain.equals(OntologyClass.owlThing) => correctDataset
        case Some(predicate) if subj.relatedClasses.contains(predicate.domain) => correctDataset
        case Some(predicate) if isDisjoined(subj, predicate.domain.asInstanceOf[OntologyClass]) => disjointDomainDataset
        case Some(predicate) => nonDisjointDomainDataset
        case None => untypedDomainDataset
      }
      case None => untypedDomainDataset
    }
  }

    /**
     * checks if two classes ar disjoint by testing any base-classes recursively for disjointnes
     * @param objClass      the type of an object
     * @param rangeClass    the range of the pertaining property
     * @param clear         new disjoint tests have to clear the relatedClasses cache, keeps already tested combinations of this cycle
     * @return
     */
  private lazy val relatedClasses = new mutable.HashSet[(OntologyClass, OntologyClass)]
  private lazy val disjoinedClassesMap = new mutable.HashMap[(OntologyClass, OntologyClass), Boolean]

  def isDisjoined(objClass : OntologyClass, rangeClass : OntologyClass, clear: Boolean = true) : Boolean = {

      if (clear)
        relatedClasses.clear()

      if(disjoinedClassesMap.keySet.contains((objClass, rangeClass)) && disjoinedClassesMap((objClass, rangeClass)))
        return true

      if(objClass.disjointWithClasses.contains(rangeClass)
        || rangeClass.disjointWithClasses.contains(objClass)) {
        disjoinedClassesMap.put((objClass, rangeClass), true)
        disjoinedClassesMap.put((rangeClass, objClass), true)
        return true
      }
      relatedClasses.add(objClass, rangeClass)
      relatedClasses.add(rangeClass, objClass)
      for (objClazz <- objClass.relatedClasses) {
        for(rangeClazz <- rangeClass.relatedClasses) {
          if (!relatedClasses.contains(objClazz, rangeClazz)) { //not!
            if (isDisjoined(objClazz, rangeClazz, clear = false))
              return true
          }
          if (!relatedClasses.contains(rangeClazz, objClazz)) { //not!
            if (isDisjoined(rangeClazz, objClazz, clear = false))
              return true
          }
        }
      }
      false
  }


  private def computeType(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Unit =
  {
    breakable {
      val classOption = ontology.classes.find(x => x._2.uri == quad.value)
      var ontoClass: OntologyClass = null
      if (classOption != null && classOption.isDefined)
        ontoClass = classOption.get._2
      else
        break
      if (!resourceTypes.contains(quad.subject)) //not! {
        resourceTypes(quad.subject) = ontoClass
      else {
        if (ontoClass.relatedClasses.contains(resourceTypes(quad.subject)))
          resourceTypes(quad.subject) = ontoClass
      }
    }
  }

  private def createDestination(finder: Finder[File], date: String, formats: scala.collection.Map[String, Formatter]) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[Dataset, Destination]()
      for (dataset <- datasets) {
        val file = finder.file(date, dataset.encoded.replace('_', '-')+'.'+suffix).get
        datasetDestinations(dataset) = new WriterDestination(writer(file), format)
      }

      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination: _*)
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }
}
