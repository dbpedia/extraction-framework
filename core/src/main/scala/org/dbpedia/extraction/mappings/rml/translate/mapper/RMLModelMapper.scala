package org.dbpedia.extraction.mappings.rml.translate.mapper

import org.dbpedia.extraction.mappings.rml.model.factory.{WikiTextBundle, WikiTextTemplateFactory}
import org.dbpedia.extraction.mappings.rml.model.{AbstractRMLModel, RMLTranslationModel}
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.GeocoordinateTemplate
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.wikiparser.TemplateNode

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(rmlModel: RMLTranslationModel) {
  
  
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
  
  def addMapping(mapping: Extractor[TemplateNode], state : MappingState) : List[RMLPredicateObjectMap] =
  {
    val language = rmlModel.wikiTitle.language.isoCode

    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => {
        ///addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
        val simplePropertyMapping = mapping.asInstanceOf[SimplePropertyMapping]
        val template = WikiTextTemplateFactory.createSimplePropertyTemplate(WikiTextBundle(simplePropertyMapping))
        val count = rmlModel.count(RMLUri.SIMPLEPROPERTYMAPPING)
        val counter = Counter(simpleProperties = count)
        TemplateAssembler.assembleTemplate(rmlModel, template, language, counter)
        List()
      }
      case "CalculateMapping" => addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => {
        //addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
        val dateIntervalMapping = mapping.asInstanceOf[DateIntervalMapping]
        val startDateTemplate = WikiTextTemplateFactory.createStartDateTemplate(WikiTextBundle(dateIntervalMapping))
        val endDateTemplate = WikiTextTemplateFactory.createEndDateTemplate(WikiTextBundle(dateIntervalMapping))
        val startDateCount = rmlModel.count(RMLUri.STARTDATEMAPPING)
        val startDateCounter = Counter(startDates = startDateCount)
        val endDateCount = rmlModel.count(RMLUri.STARTDATEMAPPING)
        val endDateCounter = Counter(endDates = endDateCount)
        TemplateAssembler.assembleTemplate(rmlModel, startDateTemplate, language, startDateCounter)
        TemplateAssembler.assembleTemplate(rmlModel, endDateTemplate, language, endDateCounter)
        List()
      }
      case "GeoCoordinatesMapping" => {
        //addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
        val geoCoordinatesMapping = mapping.asInstanceOf[GeoCoordinatesMapping]
        val template = WikiTextTemplateFactory.createGeocoordinateTemplate(WikiTextBundle(geoCoordinatesMapping))
        val count = rmlModel.count(RMLUri.LATITUDEMAPPING)
        val intermediateCount = rmlModel.count(RMLUri.INTERMEDIATEMAPPING)
        val counter = Counter(geoCoordinates = count, intermediates = intermediateCount)
        TemplateAssembler.assembleTemplate(rmlModel, template, language, counter)
        List()
      }
      case "IntermediateNodeMapping" => {
        //addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping], state)
        val intermediateMapping = mapping.asInstanceOf[IntermediateNodeMapping]
        val template = WikiTextTemplateFactory.createIntermediateTemplate(WikiTextBundle(intermediateMapping))
        val count = rmlModel.count(RMLUri.INTERMEDIATEMAPPING)
        val counter = Counter(intermediates = count)
        TemplateAssembler.assembleTemplate(rmlModel, template, language, counter)
        List()
      }
      case "ConstantMapping" => {
        //addConstantMapping(mapping.asInstanceOf[ConstantMapping])
        val constantMapping = mapping.asInstanceOf[ConstantMapping]
        val template = WikiTextTemplateFactory.createConstantTemplate(WikiTextBundle(constantMapping))
        val count = rmlModel.count(RMLUri.CONSTANTMAPPING)
        val counter = Counter(constants = count)
        TemplateAssembler.assembleTemplate(rmlModel, template, language, counter)
        List()
      }
    }
  }

  /**
    * Create mapping that is not linked yet to a triples map
    */
  def addIndependentMapping(mapping: Extractor[TemplateNode], state : MappingState) : List[RMLPredicateObjectMap] =
  {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => addIndependentSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "CalculateMapping" => addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => addIndependentDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => addIndependentGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "IntermediateNodeMapping" => addIndependentIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping], state)
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


  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping, state: MappingState) : List[RMLPredicateObjectMap] =
  {
    new IntermediateNodeMapper(rmlModel, mapping, state : MappingState).mapToModel()
  }

  def addIndependentIntermediateNodeMapping(mapping: IntermediateNodeMapping, state : MappingState) : List[RMLPredicateObjectMap] =
  {
    new IntermediateNodeMapper(rmlModel, mapping, state).addIndependentIntermediateNodeMapping()
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
