package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLPredicateObjectMap, RMLTriplesMap}
import org.dbpedia.extraction.wikiparser.TemplateNode

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(rmlModel: RMLModel) {
  
  
  def  addMappingToTriplesMap(mapping: Extractor[TemplateNode], triplesMap: RMLTriplesMap) =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => rmlMapper.addSimplePropertyMappingToTriplesMap(mapping.asInstanceOf[SimplePropertyMapping], triplesMap)
      case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
      case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
      case "DateIntervalMapping" => rmlMapper.addDateIntervalMappingToTriplesMap(mapping.asInstanceOf[DateIntervalMapping], triplesMap)
      case "GeoCoordinatesMapping" => rmlMapper.addGeoCoordinatesMappingToTriplesMap(mapping.asInstanceOf[GeoCoordinatesMapping], triplesMap)
      case "ConstantMapping" => rmlMapper.addConstantMappingToTriplesMap(mapping.asInstanceOf[ConstantMapping], triplesMap)
    }
  }
  
  def addMapping(mapping: Extractor[TemplateNode]) :List[RMLPredicateObjectMap] =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => rmlMapper.addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "CalculateMapping" => rmlMapper.addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => rmlMapper.addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => rmlMapper.addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => rmlMapper.addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "IntermediateNodeMapping" => rmlMapper.addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
      case "ConstantMapping" => rmlMapper.addConstantMapping(mapping.asInstanceOf[ConstantMapping])
    }
  }

  def addTemplateMapping(mapping: TemplateMapping) =
  {
    new TemplateRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addConditionalMapping(mapping: ConditionalMapping) : Unit =
  {
    new ConditionalRMLMapper(rmlModel, mapping).mapToModel()
  }


  def addSimplePropertyMapping(mapping: SimplePropertyMapping) : List[RMLPredicateObjectMap] =
  {
    new SimplePropertyRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addSimplePropertyMappingToTriplesMap(mapping: SimplePropertyMapping, triplesMap: RMLTriplesMap) = {
    new SimplePropertyRMLMapper(rmlModel, mapping)
      .addSimplePropertyMappingToTriplesMap(rmlModel.wikiTitle.resourceIri,triplesMap)
  }

  def addCalculateMapping(mapping: CalculateMapping) : List[RMLPredicateObjectMap] =
  {
    //TODO: implement
    println("Calculate Mappings not supported")
    null
  }


  def addCombineDateMapping(mapping: CombineDateMapping) : List[RMLPredicateObjectMap] =
  {
    //TODO: implement
    println("Combine Date Mappings not supported")
    null
  }


  def addDateIntervalMapping(mapping: DateIntervalMapping) : List[RMLPredicateObjectMap]  =
  {
    new DateIntervalRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addDateIntervalMappingToTriplesMap(mapping: DateIntervalMapping, triplesMap: RMLTriplesMap) = {
    new DateIntervalRMLMapper(rmlModel, mapping)
      .addDateIntervalMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }


  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) : List[RMLPredicateObjectMap] =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addGeoCoordinatesMappingToTriplesMap(mapping: GeoCoordinatesMapping, triplesMap: RMLTriplesMap) =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping)
      .addGeoCoordinatesMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) : List[RMLPredicateObjectMap] =
  {
    new IntermediateNodeMapper(rmlModel, mapping).mapToModel()
  }


  def addConstantMapping(mapping: ConstantMapping) : List[RMLPredicateObjectMap] =
  {
    new ConstantRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addConstantMappingToTriplesMap(mapping: ConstantMapping, triplesMap: RMLTriplesMap) =
  {
    new ConstantRMLMapper(rmlModel, mapping)
        .addConstantMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }




}
