package org.dbpedia.extraction.scripts

import java.io.{File, Writer}
import java.net.URL

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{ConfigUtils, Finder, IOUtils, Language}
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.control.Breaks._

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
  val correctDataset = DBpediaDatasets.OntologyPropertiesObjectsCleaned
  //range based datasets
  val disjointRangeDataset = DBpediaDatasets.OntologyPropertiesDisjointRange
  val untypedRangeDataset = correctDataset //new Dataset("mappingbased-properties-untyped");
  val nonDisjointRangeDataset = correctDataset //new Dataset("mappingbased-properties-non-disjoint");

  //domain based datasets
  val disjointDomainDataset = DBpediaDatasets.OntologyPropertiesDisjointDomain
  val untypedDomainDataset = correctDataset //new Dataset("mappingbased-properties-untyped");
  val nonDisjointDomainDataset = correctDataset //new Dataset("mappingbased-properties-non-disjoint");

  val datasets = Seq(correctDataset, disjointRangeDataset, disjointDomainDataset)

  val propertyMap = new scala.collection.mutable.HashMap[String, OntologyProperty]
  val disjoinedClassesMap = new mutable.HashMap[(OntologyClass, OntologyClass), Boolean]

  def main(args: Array[String]) {


    require(args != null && args.length == 2, "Two arguments required, extraction config file and extension to work with")
    require(args(0).nonEmpty, "missing required argument: config file name")
    require(args(1).nonEmpty, "missing required argument: suffix e.g. .tql.gz")


    val config = ConfigUtils.loadConfig(args(0), "UTF-8")

    val baseDir = ConfigUtils.getValue(config, "base-dir", true)(new File(_))
    if (!baseDir.exists)
      throw new IllegalArgumentException("dir " + baseDir + " does not exist")
    val langConfString = ConfigUtils.getString(config, "languages", false)
    val languages = ConfigUtils.parseLanguages(baseDir, Seq(langConfString))

    val formats = parseFormats(config, "uri-policy", "format")

    lazy val ontology = {
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

    val suffix = args(1)
    val typesDataset = "instance-types" + suffix
    val mappedTripleDataset = "mappingbased-objects-uncleaned" + suffix

    val relatedClasses = new mutable.HashSet[(OntologyClass, OntologyClass)]


    for (lang <- languages) {

      // create destination for this language
      val finder = new Finder[File](baseDir, lang, "wiki")
      val date = finder.dates().last
      val destination= createDestination(finder, date, formats)

      val resourceTypes = new scala.collection.mutable.HashMap[String, OntologyClass]()


      try {
        QuadReader.readQuads(lang.wikiCode+": Reading types from "+typesDataset, finder.file(date, typesDataset)) { quad =>
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
        QuadReader.readQuads(lang.wikiCode+": Reading types from " + mappedTripleDataset, finder.file(date, mappedTripleDataset)) { quad =>

          val rangeDataset = checkQuadRange(quad, resourceTypes, ontology)
          val domainDataset = checkQuadDomain(quad, resourceTypes, ontology)

          val datasetList = List(rangeDataset.name, domainDataset.name).distinct
          val verifiedDatasets : List[String] = {
            if (datasetList.size == 1 && datasetList.contains(correctDataset.name)) {
              datasetList
            }
            else {
              datasetList.filter(_ != correctDataset.name)
            }
          }
          for (d <- verifiedDatasets) {
            val q = quad.copy(language = lang.wikiCode, dataset = d) //set the language of the Quad
            destination.write(Seq(q))
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
     * Chacks a Quad for range violations and returns the new dataset where it should be written depending on the state
     * @param quad
     * @return
     */
    def checkQuadRange(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Dataset =
    {
      if (quad.datatype != null) {
        return correctDataset
      }

      //object is uri
      val obj = try {
        resourceTypes(quad.value)
      } catch {
        case _: Throwable => return untypedRangeDataset
      }

      val predicate = getProperty(quad.predicate, ontology)


      if (predicate != null && predicate.range.uri.trim().equals("http://www.w3.org/2002/07/owl#Thing")) {
        return correctDataset
      }
      else if (obj == null || obj == None) {
        return untypedRangeDataset
      }
      else if (predicate == null) {
        //TODO not encountered yet -> delete?
        return null
      }

      if (obj.relatedClasses.contains(predicate.range))
        return correctDataset
      else {
        if (isDisjoined(obj, predicate.range.asInstanceOf[OntologyClass], true))
          return disjointRangeDataset
        else
          return nonDisjointRangeDataset
      }

    }

    /**
      * Chacks a Quad for domain violations and returns the new dataset where it should be written depending on the state
      * @param quad
      * @return
      */
    def checkQuadDomain(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Dataset =
    {
      //object is uri
      val subj = try {
        resourceTypes(quad.subject)
      } catch {
        case _: Throwable => return untypedDomainDataset
      }

      val predicate = getProperty(quad.predicate, ontology)


      if (predicate != null && predicate.domain.uri.trim().equals("http://www.w3.org/2002/07/owl#Thing")) {
        return correctDataset
      }
      else if (subj == null || subj == None) {
        return untypedDomainDataset
      }
      else if (predicate == null) {
        //TODO not encountered yet -> delete?
        return null
      }

      if (subj.relatedClasses.contains(predicate.domain))
        return correctDataset
      else {
        if (isDisjoined(subj, predicate.domain.asInstanceOf[OntologyClass], true))
          return disjointDomainDataset
        else
          return nonDisjointDomainDataset
      }

    }

    /**
     * checks if two classes ar disjoint by testing any base-classes recursively for disjointnes
     * @param objClass      the type of an object
     * @param rangeClass    the range of the pertaining property
     * @param clear         new disjoint tests have to clear the relatedClasses cache, keeps already tested combinations of this cycle
     * @return
     */
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
            if (isDisjoined(objClazz, rangeClazz, false))
              return true
          }
          if (!relatedClasses.contains(rangeClazz, objClazz)) { //not!
            if (isDisjoined(rangeClazz, objClazz, false))
              return true
          }
        }
      }
      false
    }
  }

  private def computeType(quad: Quad, resourceTypes: scala.collection.mutable.Map[String, OntologyClass], ontology: Ontology): Unit =
  {
    breakable {
      val classOption = ontology.classes.find(x => x._2.uri == quad.value)
      var ontoClass: OntologyClass = null
      if (classOption != null && classOption != None)
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

  // returns an ontologyProperty from a URI and keeps a local cache
  private def getProperty(uri: String, ontology: Ontology) : OntologyProperty = {

    if (propertyMap.contains(uri)) {
      propertyMap(uri)
    } else {
      val predicateOpt = ontology.properties.find(x => x._2.uri == uri)
      val predicate: OntologyProperty =
        if (predicateOpt != null && predicateOpt != None) { predicateOpt.get._2 }
        else (null)
      propertyMap.put(uri, predicate);
      predicate
    }
  }

  private def createDestination(finder: Finder[File], date: String, formats: scala.collection.Map[String, Formatter]) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new HashMap[String, Destination]()
      for (dataset <- datasets) {
        val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
        datasetDestinations(dataset.name) = new WriterDestination(writer(file), format)
      }

      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination.toSeq: _*)
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }
}
