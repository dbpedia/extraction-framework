package org.dbpedia.extraction.server.resources.rml.model.factories

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.resources.rml.mappings.RMLModelMapper
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.server.resources.rml.model.{RMLModel, RMLTemplateMapping}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Factory that creates RML template mappings converted from DBpedia mappings using a triple store (Jena)
  */
class RMLTemplateMappingFactory extends RMLMappingFactory {



  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLTemplateMapping =
  {
    println("Loading RML Mapping: " + page.title.encodedWithNamespace)
    val rmlModel = new RMLModel(page.title, page.sourceUri)
    if(mappings.templateMappings.head._2.isInstanceOf[TemplateMapping]) {
      val templateMapping = mappings.templateMappings.head._2.asInstanceOf[TemplateMapping] // :|
      setupMapping(rmlModel, templateMapping)
    }
    new RMLTemplateMapping(rmlModel)
  }

  private def setupMapping(rmlModel: RMLModel, templateMapping: TemplateMapping) =
  {
    defineTriplesMap(rmlModel, templateMapping) //sets details of the triples map
    addPropertyMappings(rmlModel, templateMapping)
  }

  private def defineTriplesMap(rMLModel: RMLModel, templateMapping: TemplateMapping) =
  {
    defineSubjectMap(rMLModel, templateMapping)
    defineLogicalSource(rMLModel)
  }

  private def defineSubjectMap(rmlModel: RMLModel, templateMapping: TemplateMapping) =
  {
    rmlModel.subjectMap.addConstant(rmlModel.rmlFactory.createRMLLiteral(rmlModel.wikiTitle.resourceIri))
    rmlModel.subjectMap.addClass(rmlModel.rmlFactory.createRMLUri(templateMapping.mapToClass.uri))
    rmlModel.subjectMap.addTermTypeIRI()
    addCorrespondingPropertyAndClassToSubjectMap(rmlModel, templateMapping)
  }

  private def defineLogicalSource(rMLModel: RMLModel) =
  {
    rMLModel.logicalSource.addSource(rMLModel.rmlFactory.createRMLUri(rMLModel.sourceUri))
  }

  private def addPropertyMappings(rMLModel: RMLModel, templateMapping: TemplateMapping) =
  {
    val mapper = new RMLModelMapper(rMLModel)
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapper, mapping)
    }
  }

  private def addCorrespondingPropertyAndClassToSubjectMap(rmlModel: RMLModel, templateMapping: TemplateMapping) =
  {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = rmlModel.triplesMap.addPredicateObjectMap(new RMLUri(rmlModel.wikiTitle.resourceIri + "/CorrespondingProperty"))
      predicateObjectMap.addPredicate(new RMLUri(templateMapping.correspondingProperty.uri))
      addCorrespondingClassToPredicateObjectMap(rmlModel, predicateObjectMap: RMLPredicateObjectMap, templateMapping: TemplateMapping)
    }
  }

  private def addCorrespondingClassToPredicateObjectMap(rMLModel: RMLModel, predicateObjectMap: RMLPredicateObjectMap, templateMapping: TemplateMapping) =
  {
    if(templateMapping.correspondingClass != null) {
      val objectMap = predicateObjectMap.addObjectMap(predicateObjectMap.uri.extend("/ObjectMap"))
      val parentTriplesMap = objectMap.addParentTriplesMap(objectMap.uri.extend("/ParentTriplesMap"))
      val subjectMap = parentTriplesMap.addSubjectMap(parentTriplesMap.uri.extend("/SubjectMap"))
      subjectMap.addClass(new RMLUri(templateMapping.correspondingClass.uri))
      parentTriplesMap.addLogicalSource(rMLModel.logicalSource)
    }
  }


  private def addPropertyMapping(mapper: RMLModelMapper, mapping: PropertyMapping) =
  {

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
