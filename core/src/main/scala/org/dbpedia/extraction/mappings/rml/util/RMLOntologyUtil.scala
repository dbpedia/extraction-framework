package org.dbpedia.extraction.mappings.rml.util

import be.ugent.mmlab.rml.model.{PredicateObjectMap, TriplesMap}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty, RdfNamespace}
import org.eclipse.rdf4j.model.IRI

import scala.language.reflectiveCalls

/**
  * Loading ontology data from given ontology class instance
  */
object RMLOntologyUtil {

  // "class" is the rml value
  private final val mapToClass: String = "class"

  def loadMapToClassOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyClass =
  {
      loadTriplesMapOntologyClass(triplesMap, mapToClass, context)
  }

  def loadMapToClassOntologyViaType(predicateObjectMap: PredicateObjectMap, context: {def ontology : Ontology}): OntologyClass =
  {
      val oms = predicateObjectMap.getObjectMaps
      val om = oms.iterator().next()
      val ontologyClassName = om.getConstantValue.stringValue().replace("http://dbpedia.org/ontology/", "")
      loadOntologyClass(ontologyClassName, context)
  }

  def loadCorrespondingPropertyOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyProperty = {
      null //TODO: ? not defined in the rml mappings
  }

  def loadCorrespondingClassOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyClass = {
      null //TODO: ? not defined in the rml mappings
  }

  def loadOntologyClass(ontologyClassName : String, context: {def ontology: Ontology}): OntologyClass = {
      try {
        context.ontology.classes(ontologyClassName)
      } catch {
        case _: NoSuchElementException => throw new IllegalArgumentException("Ontology class not found: " + ontologyClassName)
      }

  }

  def loadOntologyProperty(ontologyPropertyName: String, context: {def ontology: Ontology}): OntologyProperty = {
      try {
        context.ontology.properties(ontologyPropertyName)
      } catch {
        case _ : NoSuchElementException => throw new IllegalArgumentException("Ontology property not found: " + ontologyPropertyName)
      }
  }

  def loadOntologyDataType(ontologyDataTypeName: String, context: {def ontology: Ontology}): Datatype = {
    try {
      context.ontology.datatypes(ontologyDataTypeName)
    } catch {
      case _ : NoSuchElementException => null
    }
  }

  def loadOntologyPropertyFromIRI(ontologyIRI : String, context : {def ontology: Ontology}): OntologyProperty = {
      val pattern = "(.*[/#])([^/#]+)".r
      val pattern(namespace, localname) = ontologyIRI
      val prefix = getPrefix(namespace)
      val localOntologyPropertyName = if(prefix != "dbo") {
        prefix + ":" + localname
      } else localname
      try {
        loadOntologyProperty(localOntologyPropertyName, context)
      } catch {
        case _ : IllegalArgumentException => println("Skipping Ontology Property: " + localOntologyPropertyName); null
      }
  }

  def loadOntologyClassFromIRI(ontologyIRI : String, context : {def ontology: Ontology}): OntologyClass = {
    val localOntologyClassName = ontologyIRI.replaceAll(".*/","")
    try {
      loadOntologyClass(localOntologyClassName, context)
    } catch {
      case _ : IllegalArgumentException => println("Skipping Ontology Property: " + localOntologyClassName); null
    }
  }

  def loadOntologyDataTypeFromIRI(ontologyIRI : String, context : { def ontology : Ontology}) : Datatype = {
      val localOntologyDataTypeName = ontologyIRI.replaceAll(".*/","")
      loadOntologyDataType(localOntologyDataTypeName, context)
  }


  private def loadTriplesMapOntologyClass(triplesMap: TriplesMap, ontologyType : String, context: {def ontology : Ontology}): OntologyClass = {
      val ontologyClassName = loadTriplesMapOntologyClassName(triplesMap)
      loadOntologyClass(ontologyClassName, context)
  }

  private def loadTriplesMapOntologyClassName(triplesMap: TriplesMap): String = {
    val namespace = triplesMap.getSubjectMap.getClassIRIs.toArray.head.asInstanceOf[IRI].getNamespace
    val localName = triplesMap.getSubjectMap.getClassIRIs.toArray.head.asInstanceOf[IRI].getLocalName
    if(namespace != "http://dbpedia.org/ontology/") {
      val prefix = getPrefix(namespace)
      prefix + ":" + localName
    } else {
      localName
    }
  }

  //get prefix from namespace string
  private def getPrefix(namespace: String) : String = {
    for(key <- RdfNamespace.prefixMap.keySet) {
      if(RdfNamespace.prefixMap(key).namespace == namespace) {
        return key
      }
    }
    null
  }


}
