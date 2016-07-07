package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLPredicateObjectMap, RMLUri}

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
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addConstant(rmlModel.rmlFactory.createRMLLiteral("http://mappings.dbpedia.org/wiki/resource/{{wikititle}}"))
    rmlModel.subjectMap.addClass(rmlModel.rmlFactory.createRMLUri(templateMapping.mapToClass.uri))
    rmlModel.subjectMap.addTermTypeIRI()
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() =
  {
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
  }

  private def addPropertyMappings() =
  {
    val mapper = new RMLModelMapper(rmlModel)
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapper, mapping)
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


  private def addPropertyMapping(mapper: RMLModelMapper, mapping: PropertyMapping) =
  {
    mapper.addMapping(mapping)
  }

}
