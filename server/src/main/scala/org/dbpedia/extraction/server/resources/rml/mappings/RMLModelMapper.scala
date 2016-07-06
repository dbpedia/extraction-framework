package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(rmlModel: RMLModel) {

  def addSimplePropertyMapping(mapping: SimplePropertyMapping) =
  {
    new SimplePropertyRMLMapper(rmlModel, mapping).mapToModel()
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
    new DateIntervalRMLMapper(rmlModel, mapping).mapToModel()
  }


  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) =
  {
    //new GeoCoordinatesRMLMapper(modelWrapper, mapping).mapToModel()
  }


  def addConditionalMapping(mapping: ConditionalMapping) =
  {
    //TODO: implement
    println("Conditional Mappings not supported")
  }


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) =
  {
    new IntermediateNodeMapper(rmlModel, mapping).mapToModel()
  }


  def addConstantMapping(mapping: ConstantMapping) =
  {
    //TODO: implement
    println("Constant Mappings not supported")
  }




}
