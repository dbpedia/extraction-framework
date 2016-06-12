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
    modelWrapper.addStringPropertyToResource(objectMap, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addPredicateObjectMap(mapping.ontologyProperty.uri, objectMap)
  }

  def addConstantMapping(mapping: ConstantMapping) = {
    //TODO: implement
  }

  def addCalculateMapping(mapping: CalculateMapping) = {
    //TODO: implement
  }

  def addCombineDateMapping(mapping: CombineDateMapping) = {
    //TODO: implement
  }

  def addDateIntervalMapping(mapping: DateIntervalMapping) = {
    //TODO: implement
  }

  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) = {
    //TODO: implement
  }

  def addConditionalMapping(mapping: ConditionalMapping) = {
    //TODO: implement
  }

  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) = {
    //TODO: implement
  }
  
}
