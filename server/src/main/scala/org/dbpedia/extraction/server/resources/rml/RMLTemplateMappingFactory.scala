package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.mappings.RMLModelMapper
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {

  private var templateMapping: TemplateMapping = null
  private var mapper: RMLModelMapper = null


  /**
    * Creates the context in this factory and creates the mapping from it
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLTemplateMapping =
  {
    this.page = page
    this.language = language
    this.templateMapping = mappings.templateMappings.head._2.asInstanceOf[TemplateMapping] // :|
    createMapping()
  }

  /**
    * Create the mapping
    */
  private def createMapping(): RMLTemplateMapping =
  {
    createNewModelWithTriplesMap()
    defineTriplesMap() //sets details of the triples map
    addPropertyMappings()
    createRMLTemplateMapping
  }

  private def defineTriplesMap() =
  {
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    modelWrapper.addPropertyAsPropertyToResource(modelWrapper.subjectMap, RdfNamespace.RR.namespace + "constant", page.title.resourceIri)
    modelWrapper.addPropertyAsPropertyToResource(modelWrapper.subjectMap, RdfNamespace.RR.namespace + "class", templateMapping.mapToClass.uri)
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() =
  {
    modelWrapper.addPropertyAsPropertyToResource(modelWrapper.logicalSource, RdfNamespace.RML.namespace + "source", page.sourceUri)
  }

  private def addPropertyMappings() =
  {
    updateModelMapper()
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapping)
    }
  }

  private def updateModelMapper() =
  {
    mapper = new RMLModelMapper(modelWrapper)
  }

  private def addCorrespondingPropertyAndClassToSubjectMap() =
  {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = modelWrapper.addPredicateObjectMap("correspondingProperty")
      modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "predicate", templateMapping.correspondingProperty.uri)
      addCorrespondingClassToPredicateObjectMap(predicateObjectMap)
    }
  }

  private def addCorrespondingClassToPredicateObjectMap(predicateObjectMap: Resource) =
  {
    if(templateMapping.correspondingClass != null) {
      val objectMap = modelWrapper.addBlankNode()
      modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
      val parentTriplesMap = modelWrapper.addBlankNode()
      modelWrapper.addResourceAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", parentTriplesMap)

      //add subject map to parent triples map
      val subjectMap = modelWrapper.addBlankNode()
      modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RR.namespace + "subjectMap", subjectMap)

      //add class to subject map
      modelWrapper.addPropertyAsPropertyToResource(subjectMap, RdfNamespace.RR.namespace + "class", templateMapping.correspondingClass.uri)

      //add logical source to subject to parent triples map
      modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RML.namespace + "logicalSource", modelWrapper.logicalSource)
    }
  }


  private def addPropertyMapping(mapping: PropertyMapping) =
  {

    //println(mapping.getClass.getSimpleName) //TODO: remove, this is for debug purposes

    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => mapper.addSimplePropertyMapping(mapping.asInstanceOf[SimplePropertyMapping])
      case "CalculateMapping" => mapper.addCalculateMapping(mapping.asInstanceOf[CalculateMapping])
      case "CombineDateMapping" => mapper.addCombineDateMapping(mapping.asInstanceOf[CombineDateMapping])
      case "DateIntervalMapping" => mapper.addDateIntervalMapping(mapping.asInstanceOf[DateIntervalMapping])
      case "GeoCoordinatesMapping" => mapper.addGeoCoordinatesMapping(mapping.asInstanceOf[GeoCoordinatesMapping])
      case "ConditionalMapping" => mapper.addConditionalMapping(mapping.asInstanceOf[ConditionalMapping])
      case "IntermediateNodeMapping" => mapper.addIntermediateNodeMapping(mapping.asInstanceOf[IntermediateNodeMapping])
      case "ConstantMapping" => mapper.addConstantMapping(mapping.asInstanceOf[ConstantMapping])
    }
  }

}
