package org.dbpedia.extraction.mappings.rml

import be.ugent.mmlab.rml.model.TriplesMap
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.openrdf.model.URI
import scala.language.reflectiveCalls

object RMLOntologyLoader {

  private final val mapToClass: String = "class"

  def loadMapToClassOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyClass = {
      loadOntologyClass(triplesMap, mapToClass, context)
  }

  def loadCorrespondingPropertyOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyProperty = {
      null //TODO: ?
  }

  def loadCorrespondingClassOntology(triplesMap: TriplesMap, context: {def ontology : Ontology}): OntologyClass = {
      null //TODO: ?
  }

  def loadOntologyClass(triplesMap: TriplesMap, ontologyType : String, context: {def ontology : Ontology}): OntologyClass = {
      val ontologyClassName = loadOntologyClassName(triplesMap)
      context.ontology.classes(ontologyClassName)
  }

  private def loadOntologyClassName(triplesMap: TriplesMap): String = {
      triplesMap.getSubjectMap.getClassIRIs.toArray.head.asInstanceOf[URI].getLocalName
  }
}
