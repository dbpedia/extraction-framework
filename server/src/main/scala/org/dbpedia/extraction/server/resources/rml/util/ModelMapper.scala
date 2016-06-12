package org.dbpedia.extraction.server.resources.rml.util

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}

/**
  * Class that adds mappings to a ModelWrapper
  */
class ModelMapper(modelWrapper: ModelWrapper) {

  val triplesMap: Resource = modelWrapper.getRoot

  def addSimplePropertyMapping(mapping: SimplePropertyMapping) = {
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addLiteralPropertyToResource(objectMap, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addLiteralPropertyToResource(objectMap, Prefixes("rml") + "languageMap", mapping.language.name)
    modelWrapper.addPredicateObjectMapToRoot(mapping.ontologyProperty.uri, objectMap)
  }

  def addCalculateMapping(mapping: CalculateMapping) = {
    //TODO: implement
  }

  def addCombineDateMapping(mapping: CombineDateMapping) = {
    //TODO: implement
  }

  def addDateIntervalMapping(mapping: DateIntervalMapping) = {
    val dateIntervalPom = modelWrapper.addBlankNode()
    modelWrapper.addResourcePropertyToResource(modelWrapper.getRoot, Prefixes("rr") + "predicateObjectMap", dateIntervalPom)
    val object1 = modelWrapper.addBlankNode()
    modelWrapper.addLiteralPropertyToResource(object1, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addPredicateObjectMapToResource(dateIntervalPom, mapping.startDateOntologyProperty.uri, object1)
    val object2 = modelWrapper.addBlankNode()
    modelWrapper.addLiteralPropertyToResource(object2, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addPredicateObjectMapToResource(dateIntervalPom, mapping.endDateOntologyProperty.uri, object2)
  }

  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) = {
    if(mapping.coordinates != null) {
      val objectMap1 = modelWrapper.addBlankNode()
      modelWrapper.addLiteralPropertyToResource(objectMap1, Prefixes("rr") + "parentTriplesMap", mapping.coordinates)
      modelWrapper.addPredicateObjectMapToRoot(Prefixes("dbo") + "coordinates", objectMap1)
    } else if (mapping.latitude != null && mapping.longitude != null) {
      //TODO: implement
    }
  }

  def addConditionalMapping(mapping: ConditionalMapping) = {
    //TODO: implement
  }

  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) = {
    //TODO: implement
  }
  
}
