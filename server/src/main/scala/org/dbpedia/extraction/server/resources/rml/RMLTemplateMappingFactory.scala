package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.server.resources.rml.util.ModelMapper
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, TemplateNode, WikiTitle}

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {

  private var templateMapping: TemplateMapping = null
  private val mapper: ModelMapper = new ModelMapper(modelWrapper)


  /**
    * Creates the context for the converted mapping and creates the mapping from it
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLTemplateMapping = {
    this.page = page
    this.language = language
    this.templateMapping = mappings.templateMappings.head._2.asInstanceOf[TemplateMapping] // :|
    createMapping()
  }

  private def createMapping(): RMLTemplateMapping = {
    createNewTriplesMap()
    defineTriplesMap() //sets details of the triples map
    addPropertyMappings()
    createRMLTemplateMapping
  }

  private def defineTriplesMap() = {
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() = {
    addConstantToSubjectMap()
    addMapToClassToSubjectMap()
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() = {
    addSourceToLogicalSource()
  }

  private def addPropertyMappings() = {
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapping)
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

  private def addConstantToSubjectMap() = {
    modelWrapper.addStringPropertyToResource(subjectMap, prefixes("rr") + "constant", page.title.encoded.toString)
  }

  private def addSourceToLogicalSource() = {
    modelWrapper.addPropertyToResource(logicalSource, prefixes("rr") + "source", page.sourceUri)
  }

  private def addMapToClassToSubjectMap() = {
    modelWrapper.addPropertyToResource(subjectMap, prefixes("rr") + "class", templateMapping.mapToClass.uri)
  }

  private def addCorrespondingClassToSubjectMap(predicateObjectMap: Resource) = {
    if(templateMapping.correspondingClass != null) {
      val objectMap = modelWrapper.addPropertyResource(null)
      modelWrapper.addResourcePropertyToResource(predicateObjectMap, prefixes("rr") + "objectMap", objectMap)
      val parentTriplesMap = modelWrapper.addPropertyResource(null)
      modelWrapper.addResourcePropertyToResource(objectMap, prefixes("rr") + "parentTriplesMap", parentTriplesMap)
      val subjectMap = modelWrapper.addPropertyResource(null)
      modelWrapper.addResourcePropertyToResource(parentTriplesMap,prefixes("rr") + "subjectMap", subjectMap)
      modelWrapper.addPropertyToResource(subjectMap, prefixes("rr") + "class", templateMapping.correspondingClass.name)
    }
  }

  private def addCorrespondingPropertyAndClassToSubjectMap() = {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = modelWrapper.addPropertyResource(null)
      modelWrapper.addPropertyToResource(predicateObjectMap, prefixes("rr") + "predicate", templateMapping.correspondingProperty.uri)
      modelWrapper.addResourcePropertyToResource(subjectMap, prefixes("rr") + "predicateObjectMap", predicateObjectMap)
      addCorrespondingClassToSubjectMap(predicateObjectMap)
    }
  }

}
