package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(modelWrapper: RMLModelWrapper) {


  def addSimplePropertyMapping(mapping: SimplePropertyMapping) = {
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addLiteralAsPropertyToResource(objectMap, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addLiteralAsPropertyToResource(objectMap, Prefixes("rml") + "languageMap", mapping.language.name)
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
    modelWrapper.addResourceAsPropertyToResource(modelWrapper.triplesMap, Prefixes("rr") + "predicateObjectMap", dateIntervalPom)

    val object1 = modelWrapper.addBlankNode()
    modelWrapper.addLiteralAsPropertyToResource(object1, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addPredicateObjectMapToResource(dateIntervalPom, mapping.startDateOntologyProperty.uri, object1)

    val object2 = modelWrapper.addBlankNode()
    modelWrapper.addLiteralAsPropertyToResource(object2, Prefixes("rml") + "reference", mapping.templateProperty)
    modelWrapper.addPredicateObjectMapToResource(dateIntervalPom, mapping.endDateOntologyProperty.uri, object2)
  }

  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) = {
    if(mapping.coordinates != null) {
      val objectMap1 = modelWrapper.addBlankNode()
      modelWrapper.addLiteralAsPropertyToResource(objectMap1, Prefixes("rr") + "parentTriplesMap", mapping.coordinates)
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
