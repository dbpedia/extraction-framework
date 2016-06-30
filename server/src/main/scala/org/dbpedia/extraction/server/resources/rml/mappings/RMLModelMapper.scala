package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.RMLModelWrapper

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(modelWrapper: RMLModelWrapper) {


  //TODO: this needs to be refactored into seperate classes

  def addSimplePropertyMapping(mapping: SimplePropertyMapping) =
  {
    new SimplePropertyRMLMapper(modelWrapper, mapping).mapToModel()
  }

  def addCalculateMapping(mapping: CalculateMapping) =
  {
    //TODO: implement
    println("Calculate Mappings not supported")
  }


  def addCombineDateMapping(mapping: CombineDateMapping) =
  {
    //TODO: implement
    println("Combine Date Mappings not supported")
  }


  def addDateIntervalMapping(mapping: DateIntervalMapping) =
  {
    new DateIntervalRMLMapper(modelWrapper, mapping).mapToModel()
  }


  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) =
  {
    new GeoCoordinatesRMLMapper(modelWrapper, mapping).mapToModel()
  }


  def addConditionalMapping(mapping: ConditionalMapping) =
  {
    //TODO: implement
    println("Conditional Mappings not supported")
  }


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) =
  {
    new IntermediateNodeMapper(modelWrapper, mapping).mapToModel()
  }


  def addConstantMapping(mapping: ConstantMapping) =
  {
    //TODO: implement
    println("Constant Mappings not supported")
  }




}
