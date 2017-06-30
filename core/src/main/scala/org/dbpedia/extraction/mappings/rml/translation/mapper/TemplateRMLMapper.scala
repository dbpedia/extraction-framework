package org.dbpedia.extraction.mappings.rml.translation.mapper

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.translation.model.RMLModel
import org.dbpedia.extraction.mappings.rml.translation.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLSubjectMap, RMLUri}

/**
  * Creates an RML Template Mapping
  */
class TemplateRMLMapper(rmlModel: RMLModel, templateMapping: TemplateMapping) {

  def mapToModel() = {
    defineTriplesMap() //sets details of the triples map
    addPropertyMappings()
  }

  private def defineTriplesMap() =
  {
    rmlModel.triplesMap.addDCTermsType(new RMLLiteral("templateMapping"))
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addTemplate(rmlModel.rmlFactory.createRMLLiteral("http://en.dbpedia.org/resource/{wikititle}"))
    rmlModel.subjectMap.addClass(rmlModel.rmlFactory.createRMLUri(templateMapping.mapToClass.uri))
    addExtraClassesToSubjectMap(rmlModel.subjectMap)
    rmlModel.subjectMap.addIRITermType()
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() =
  {
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
  }

  private def addPropertyMappings() =
  {
    val state = new MappingState
    val mapper = new RMLModelMapper(rmlModel)
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapper, mapping, state)
    }
  }

  private def addCorrespondingPropertyAndClassToSubjectMap() =
  {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = rmlModel.triplesMap.addPredicateObjectMap(new RMLUri(rmlModel.wikiTitle.resourceIri + "/CorrespondingProperty"))
      predicateObjectMap.addPredicate(new RMLUri(templateMapping.correspondingProperty.uri))
      addCorrespondingClassToPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap)
    }
  }

  private def addCorrespondingClassToPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap) =
  {
    if(templateMapping.correspondingClass != null) {
      val objectMap = predicateObjectMap.addObjectMap(predicateObjectMap.uri.extend("/ObjectMap"))
      val parentTriplesMap = objectMap.addParentTriplesMap(objectMap.uri.extend("/ParentTriplesMap"))
      val subjectMap = parentTriplesMap.addSubjectMap(parentTriplesMap.uri.extend("/SubjectMap"))
      subjectMap.addClass(new RMLUri(templateMapping.correspondingClass.uri))
      parentTriplesMap.addLogicalSource(rmlModel.logicalSource)
    }
  }

  /**
    * Add related classes to the subject map
    *
    * @param subjectMap
    */
  private def addExtraClassesToSubjectMap(subjectMap: RMLSubjectMap) =
  {
    val relatedClasses = templateMapping.mapToClass.relatedClasses
    for(cls <- relatedClasses) {
      if(!cls.uri.contains("%3E")) {
        subjectMap.addClass(new RMLUri(cls.uri))
      }
    }
  }


  private def addPropertyMapping(mapper: RMLModelMapper, mapping: PropertyMapping, state: MappingState) =
  {
    mapper.addMapping(mapping, state)
  }

}
