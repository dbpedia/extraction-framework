package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, TemplateNode, WikiTitle}

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {

  private var templateMapping: TemplateMapping = null


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

  private def addConstantToSubjectMap() = {
    addStringPropertyToResource(subjectMap, prefixes("rr") + "constant", page.title.encoded.toString)
  }

  private def addSourceToLogicalSource() = {
    //TODO: implement
  }

  private def addMapToClassToSubjectMap() = {
    val objectMap = addPropertyResource(null, templateMapping.mapToClass.uri)
    if(templateMapping.correspondingProperty != null) {
      addPropertyToResource(objectMap, prefixes("rr") + "predicate", templateMapping.correspondingProperty.uri)
      val objectMap2 = addPropertyResource(null)
      val objectMap3 = addPropertyResource(null)
      val subjectMap = addPropertyResource(null)
      addResourcePropertyToResource(objectMap, prefixes("rr") + "objectMap", objectMap2)
      addResourcePropertyToResource(objectMap2, prefixes("rr") + "parentTriplesMap", objectMap3)
      addResourcePropertyToResource(objectMap3, prefixes("rr") + "subjectMap", subjectMap)
      addPropertyToResource(subjectMap, prefixes("rr") + "class", templateMapping.correspondingClass.uri)
    }
    addResourcePropertyToResource(subjectMap, prefixes("rr") + "predicateObjectMap", objectMap)
  }

  private def addCorrespondingClassToSubjectMap() = {
    //TODO:implement
  }

  private def addCorrespondingPropertyToSubjectMap() = {
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
