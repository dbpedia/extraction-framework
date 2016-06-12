package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.resources.rml.util.{ModelMapper, Prefixes}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {

  private var templateMapping: TemplateMapping = null
  private var mapper: ModelMapper = null


  /**
    * Creates the context for the converted mapping and creates the mapping from it
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLTemplateMapping = {
    this.page = page
    this.language = language
    this.templateMapping = mappings.templateMappings.head._2.asInstanceOf[TemplateMapping] // :|
    createMapping()
  }

  /**
    * Create the mapping
    */
  private def createMapping(): RMLTemplateMapping = {
    createNewTriplesMap()
    defineTriplesMap() //sets details of the triples map
    mapper = new ModelMapper(modelWrapper) //modelWrapper updated with new root
    addPropertyMappings()
    createRMLTemplateMapping
  }

  private def defineTriplesMap() = {
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() = {
    modelWrapper.addStringPropertyToResource(subjectMap, Prefixes("rr") + "constant", page.title.encoded.toString)
    modelWrapper.addPropertyToResource(subjectMap, Prefixes("rr") + "class", templateMapping.mapToClass.uri)
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() = {
    modelWrapper.addPropertyToResource(logicalSource, Prefixes("rml") + "source", page.sourceUri)
  }

  private def addPropertyMappings() = {
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapping)
    }
  }

  private def addCorrespondingClassToSubjectMap(predicateObjectMap: Resource) = {
    if(templateMapping.correspondingClass != null) {
      val objectMap = modelWrapper.addBlankNode()
      modelWrapper.addResourcePropertyToResource(predicateObjectMap, Prefixes("rr") + "objectMap", objectMap)
      val parentTriplesMap = modelWrapper.addBlankNode()
      modelWrapper.addResourcePropertyToResource(objectMap, Prefixes("rr") + "parentTriplesMap", parentTriplesMap)
      val subjectMap = modelWrapper.addBlankNode()
      modelWrapper.addResourcePropertyToResource(parentTriplesMap, Prefixes("rr") + "subjectMap", subjectMap)
      modelWrapper.addPropertyToResource(subjectMap, Prefixes("rr") + "class", templateMapping.correspondingClass.uri)
    }
  }

  private def addCorrespondingPropertyAndClassToSubjectMap() = {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = modelWrapper.addBlankNode()
      modelWrapper.addPropertyToResource(predicateObjectMap, Prefixes("rr") + "predicate", templateMapping.correspondingProperty.uri)
      modelWrapper.addResourcePropertyToResource(subjectMap, Prefixes("rr") + "predicateObjectMap", predicateObjectMap)
      addCorrespondingClassToSubjectMap(predicateObjectMap)
    }
  }

  private def addPropertyMapping(mapping: PropertyMapping) = {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => mapper.addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "ConstantMapping" => mapper.addConstantMapping(mapping.asInstanceOf[ConstantMapping])
      case "CalculateMapping" => mapper.addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => mapper.addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => mapper.addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => mapper.addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "ConditionalMapping" => mapper.addConditionalMapping(mapping.asInstanceOf[ConditionalMapping])
      case "IntermediateNodeMapping" => mapper.addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
    }
  }

}
