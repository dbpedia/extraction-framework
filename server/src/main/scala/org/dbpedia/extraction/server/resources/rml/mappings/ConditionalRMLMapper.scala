package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources._

/**
  * Creates RML Mapping from ConditionalMappings and adds the triples to the given model
  */
class ConditionalRMLMapper(rmlModel: RMLModel, mapping: ConditionalMapping) {

  private val rmlMapper = new RMLModelMapper(rmlModel)
  private var rmlFactory = rmlModel.rmlFactory

  def mapToModel() = {
    defineTriplesMap()
    addDefaultMappings()
    addConditions()
  }

  def addConditions() = {

    val firstConditionMapping = mapping.cases(0)
    val firstTemplateMapping = firstConditionMapping.mapping.asInstanceOf[TemplateMapping]

    //add first mapToClass
    val mapToClassPomUri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/ConditionalMapping" + "/" +
      firstConditionMapping.templateProperty + "_" + firstConditionMapping.operator + "_" + firstConditionMapping.value)
    val mapToClassPom = rmlModel.triplesMap.addConditionalPredicateObjectMap(mapToClassPomUri)
    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    val mapToClassOmUri = mapToClassPomUri.extend("/ObjectMap")
    val mapToClassOm = mapToClassPom.addObjectMap(mapToClassOmUri)

    mapToClassOm.addConstant(new RMLUri(firstTemplateMapping.mapToClass.uri))
    val conditionFunctionTermMap = addEqualCondition(firstConditionMapping, mapToClassPom)

    //add predicate object maps
    val rmlMapper = new RMLModelMapper(rmlModel)
    for(propertyMapping <- firstTemplateMapping.mappings)
    {
      val pomList = rmlMapper.addMapping(propertyMapping)
      for(pom <- pomList)
      {
        val condPom = rmlModel.rmlFactory.transformToConditional(pom)
        condPom.addEqualCondition(conditionFunctionTermMap)
      }
    }

    //continue recursively
    addCondition(1, mapToClassPom)

  }

  //recursively add conditions
  def addCondition(index: Int, predicateObjectMap: RMLConditionalPredicateObjectMap) : Unit =
  {

    if(index == mapping.cases.size) {
      return
    }

    val condition = mapping.cases(index)
    val templateMapping = condition.mapping.asInstanceOf[TemplateMapping]

    val mapToClassPomUri = new RMLUri(predicateObjectMap.resource.getURI).extend("/" + index)
    val mapToClassPom = predicateObjectMap.addFallbackMap(mapToClassPomUri)
    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    val conditionFunctionTermMap = addEqualCondition(condition, mapToClassPom)

    for(propertyMapping <- templateMapping.mappings)
    {
      val pomList = rmlMapper.addIndependentMapping(propertyMapping)
      for(pom <- pomList)
      {
        val condPom = rmlFactory.transformToConditional(pom)
        condPom.addEqualCondition(conditionFunctionTermMap)
        mapToClassPom.addFallbackMap(condPom)
      }

    }

    addCondition(index + 1, mapToClassPom)

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


  def addDefaultMappings() =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    for(defaultMapping <- mapping.defaultMappings) {
      rmlMapper.addMapping(defaultMapping)
    }
  }


  private def addEqualCondition(conditionMapping: ConditionMapping, pom: RMLConditionalPredicateObjectMap) : RMLFunctionTermMap =
  {
    val functionTermMapUri = new RMLUri(pom.resource.getURI).extend("/FunctionTermMap")
    val functionTermMap = pom.addEqualCondition(functionTermMapUri)
    functionTermMap
  }

}
