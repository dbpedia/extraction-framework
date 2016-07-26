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
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => addSimplePropertyMappingToTriplesMap(mapping.asInstanceOf[SimplePropertyMapping], triplesMap)
      case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
      case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
      case "DateIntervalMapping" => addDateIntervalMappingToTriplesMap(mapping.asInstanceOf[DateIntervalMapping], triplesMap)
      case "GeoCoordinatesMapping" => addGeoCoordinatesMappingToTriplesMap(mapping.asInstanceOf[GeoCoordinatesMapping], triplesMap)
      case "ConstantMapping" => addConstantMappingToTriplesMap(mapping.asInstanceOf[ConstantMapping], triplesMap)
    }
  }
  
  def addMapping(mapping: Extractor[TemplateNode]) :List[RMLPredicateObjectMap] =
  {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "CalculateMapping" => addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "IntermediateNodeMapping" => addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
      case "ConstantMapping" => addConstantMapping(mapping.asInstanceOf[ConstantMapping])
    }
  }

  /**
    * Create mapping that is not linked yet to a triples map
    */
  def addIndependentMapping(mapping: Extractor[TemplateNode]) : List[RMLPredicateObjectMap] =
  {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => addIndependentSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "CalculateMapping" => addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => addIndependentDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => addIndependentGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "IntermediateNodeMapping" => addIndependentIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
      case "ConstantMapping" => addIndependentConstantMapping(mapping.asInstanceOf[ConstantMapping])
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

  def addSimplePropertyMappingToTriplesMap(mapping: SimplePropertyMapping, triplesMap: RMLTriplesMap) =
  {
    new SimplePropertyRMLMapper(rmlModel, mapping)
      .addSimplePropertyMappingToTriplesMap(rmlModel.wikiTitle.resourceIri,triplesMap)
  }

  def addIndependentSimplePropertyMapping(mapping: SimplePropertyMapping) : List[RMLPredicateObjectMap] =
  {
    new SimplePropertyRMLMapper(rmlModel, mapping).addIndependentSimplePropertyMapper()
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

  def addDateIntervalMappingToTriplesMap(mapping: DateIntervalMapping, triplesMap: RMLTriplesMap) =
  {
    new DateIntervalRMLMapper(rmlModel, mapping)
      .addDateIntervalMappingToTriplesMap(rmlModel.wikiTitle.resourceIri, triplesMap)
  }

  def addIndependentDateIntervalMapping(mapping: DateIntervalMapping) : List[RMLPredicateObjectMap] =
  {
    new DateIntervalRMLMapper(rmlModel, mapping).addIndependentDateIntervalMapping()
  }


  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) : List[RMLPredicateObjectMap] =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping).mapToModel()
  }

  def addGeoCoordinatesMappingToTriplesMap(mapping: GeoCoordinatesMapping, triplesMap: RMLTriplesMap) =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping)
      .addGeoCoordinatesMappingToTriplesMap(triplesMap)
  }

  def addIndependentGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) : List[RMLPredicateObjectMap] =
  {
    new GeoCoordinatesRMLMapper(rmlModel, mapping).addIndependentGeoCoordinatesMapping()
  }


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) : List[RMLPredicateObjectMap] =
  {
    new IntermediateNodeMapper(rmlModel, mapping).mapToModel()
  }

  def addIndependentIntermediateNodeMapping(mapping: IntermediateNodeMapping) : List[RMLPredicateObjectMap] =
  {
    new IntermediateNodeMapper(rmlModel, mapping).addIndependentIntermediateNodeMapping()
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

  def addIndependentConstantMapping(mapping: ConstantMapping) : List[RMLPredicateObjectMap] =
  {
    new ConstantRMLMapper(rmlModel, mapping)
        .addIndependentConstantMapping(rmlModel.wikiTitle.resourceIri)
  }




}
