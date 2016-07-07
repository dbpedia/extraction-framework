package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLFunctionTermMap, RMLObjectMap, RMLPredicateObjectMap, RMLUri}

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

    //add first mapToClass

    val mapToClassPomUri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/ConditionalMapping")
    val mapToClassPom = rmlModel.triplesMap.addPredicateObjectMap(mapToClassPomUri)
    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    val mapToClassOmUri = mapToClassPomUri.extend("/ObjectMapUri")
    val mapToClassOm = mapToClassPom.addObjectMap(mapToClassOmUri)
    val firstConditionMapping = mapping.cases(0)
    val firstTemplateMapping = firstConditionMapping.mapping.asInstanceOf[TemplateMapping]
    mapToClassOm.addConstant(new RMLUri(firstTemplateMapping.mapToClass.uri))
    val conditionFunctionTermMap = addEqualCondition(firstConditionMapping, mapToClassOm)

    //add predicate object maps
    for(propertyMapping <- firstTemplateMapping.mappings)
    {

    }

    //continue recursively
    addCondition(1, mapToClassPom)

  }

  private def defineTriplesMap() =
  {
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addConstant(rmlModel.rmlFactory.createRMLLiteral("http://mappings.dbpedia.org/wiki/resource/{{wikititle}}"))
  }

  private def defineLogicalSource() =
  {
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
  }

  def addCondition(index: Int, predicateObjectMap: RMLPredicateObjectMap) : Unit =
  {
    val condition = mapping.cases(index)
    val templateMapping = condition.mapping.asInstanceOf[TemplateMapping]

  }


  def addDefaultMappings() = {
    val rmlMapper = new RMLModelMapper(rmlModel)
    for(defaultMapping <- mapping.defaultMappings) {
      rmlMapper.addMapping(defaultMapping)
    }

  }


  private def addEqualCondition(conditionMapping: ConditionMapping, objectMap: RMLObjectMap) : RMLFunctionTermMap = {
    null
  }

}
