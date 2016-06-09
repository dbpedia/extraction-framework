package org.dbpedia.extraction.server.resources.rml

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

/**
  * Factory that creates RML Template Mappings converted from DBpedia mappings using a triple store
  */
class RMLTemplateMappingFactory(page: PageNode, language: Language, templateMapping : TemplateMapping) extends RMLMappingFactory {


  //TODO: initiate triple store

  /**
    * Creates the converted mapping
    */
  def createMapping(): RMLTemplateMapping = {
    defineTriplesMap()
    defineLogicalSource()
    defineSubjectMap()
    addPropertyMappings()
    new RMLTemplateMapping()
  }


  private def defineTriplesMap() = {
    addLogicalSourceToTriplesMap()
    addSubjectMapToTriplesMap()
  }

  private def defineLogicalSource() = {
    addSourceToLogicalSource()
  }

  private def defineSubjectMap() = {
    addConstantToSubjectMap()
    addCorrespondingClass()
    addCorrespondingProperty()
    addMapToClass()
  }


  private def addPropertyMappings() = {
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapping)
    }
  }

  private def addPropertyMapping(mapping: PropertyMapping) = {
    mapping.getClass.getName match {
      case "SimplePropertyMapping" => addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "ConstantMapping" => addConstantMapping(mapping.asInstanceOf[ConstantMapping])
      case "CalculateMapping" => addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "ConditionalMapping" => addConditionalMapping(mapping.asInstanceOf[ConditionalMapping])
      case "IntermediateNodeMapping" => addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
    }
  }

  private def addSubjectMapToTriplesMap() = {
    //TODO: implement
  }

  private def addLogicalSourceToTriplesMap() = {
    //TODO: implement
  }

  private def addConstantToSubjectMap() = {
    //TODO: implement
  }

  private def addSourceToLogicalSource() = {
    //TODO: implement
  }

  private def addMapToClass() = {
    //TODO: implement
  }

  private def addCorrespondingClass() = {
    //TODO: implement
  }

  private def addCorrespondingProperty() = {
    //TODO: implement
  }

  private def addSimplePropertyMapping(mapping: SimplePropertyMapping) = {
    //TODO: implement
  }

  private def addConstantMapping(mapping: ConstantMapping) = {
    //TODO: implement
  }

  private def addCalculateMapping(mapping: CalculateMapping) = {
    //TODO: implement
  }

  private def addCombineDateMapping(mapping: CombineDateMapping) = {
    //TODO: implement
  }

  private def addDateIntervalMapping(mapping: DateIntervalMapping) = {
    //TODO: implement
  }

  private def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) = {
    //TODO: implement
  }

  private def addConditionalMapping(mapping: ConditionalMapping) = {
    //TODO: implement
  }

  private def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) = {
    //TODO: implement
  }

}
