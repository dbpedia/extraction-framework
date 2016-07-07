package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

/**
  * Creates RML Mapping from ConditionalMappings and adds the triples to the given model
  */
class ConditionalRMLMapper(rmlModel: RMLModel, mapping: ConditionalMapping) {

  def mapToModel() = {
    defineTriplesMap()
    addDefaultMappings()
    addConditions()
  }

  def addConditions() = {
    for(conditionMapping <- mapping.cases) {
      addCondition(conditionMapping)
    }
  }

  private def defineTriplesMap() =
  {
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addConstant(rmlModel.rmlFactory.createRMLLiteral("http://mappings.dbpedia.org/wiki/resource/{{wikititle}}"))
    rmlModel.subjectMap.addTermTypeIRI()
  }

  private def defineLogicalSource() =
  {
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
  }

  def addCondition(conditionMapping: ConditionMapping) =
  {
    println(conditionMapping.mapping.asInstanceOf[TemplateMapping].mapToClass)
  }


  def addDefaultMappings() = {
    val rmlModelMapper = new RMLModelMapper(rmlModel)
    for(defaultMapping <- mapping.defaultMappings) {
      defaultMapping.getClass.getSimpleName match {
        case "SimplePropertyMapping" => rmlModelMapper.addSimplePropertyMapping(defaultMapping.asInstanceOf[SimplePropertyMapping])
        case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
        case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
        case "DateIntervalMapping" => rmlModelMapper.addDateIntervalMapping(defaultMapping.asInstanceOf[DateIntervalMapping])
        case "GeoCoordinatesMapping" => rmlModelMapper.addGeoCoordinatesMapping(defaultMapping.asInstanceOf[GeoCoordinatesMapping])
        case "IntermediateNodeMapping" => rmlModelMapper.addIntermediateNodeMapping(defaultMapping.asInstanceOf[IntermediateNodeMapping])
        case "ConstantMapping" => rmlModelMapper.addConstantMapping(defaultMapping.asInstanceOf[ConstantMapping])
      }
    }

  }

}
