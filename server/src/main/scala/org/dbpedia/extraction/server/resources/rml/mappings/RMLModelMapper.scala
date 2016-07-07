package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.RMLTriplesMap

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(rmlModel: RMLModel) {


  def addTemplateMapping(mapping: TemplateMapping) =
  {
    new TemplateRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addConditionalMapping(mapping: ConditionalMapping) =
  {
    new ConditionalRMLMapper(rmlModel, mapping).mapToModel()
  }


  def addSimplePropertyMapping(mapping: SimplePropertyMapping) =
  {
    new SimplePropertyRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addSimplePropertyMappingToTriplesMap(mapping: SimplePropertyMapping, triplesMap: RMLTriplesMap) = {
    new SimplePropertyRMLMapper(rmlModel, mapping)
      .addSimplePropertyMappingToTriplesMap(rmlModel.wikiTitle.resourceIri,triplesMap)
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

  def addDateIntervalMappingToTriplesMap(mapping: DateIntervalMapping, triplesMap: RMLTriplesMap) = {
    new DateIntervalRMLMapper(rmlModel, mapping)
      .addDateIntervalMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }


  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addGeoCoordinatesMappingToTriplesMap(mapping: GeoCoordinatesMapping, triplesMap: RMLTriplesMap) =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping)
      .addGeoCoordinatesMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) =
  {
    new IntermediateNodeMapper(rmlModel, mapping).mapToModel()
  }


  def addConstantMapping(mapping: ConstantMapping) =
  {
    new ConstantRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addConstantMappingToTriplesMap(mapping: ConstantMapping, triplesMap: RMLTriplesMap) =
  {
    new ConstantRMLMapper(rmlModel, mapping)
        .addConstantMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }




}
