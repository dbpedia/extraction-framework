package org.dbpedia.extraction.mappings.rml.translate.mapper

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.model.{RMLModel, RMLTranslationModel}
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, RdfNamespace}

import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from ConditionalMappings and adds the triples to the given model
  *
  */
class ConditionalRMLMapper(rmlModel: RMLTranslationModel, mapping: ConditionalMapping) {

  private val rmlMapper = new RMLModelMapper(rmlModel)
  private val rmlFactory = rmlModel.rmlFactory

  def mapToModel() = {
    defineTriplesMap()
    addDefaultMappings()
    addConditions()
  }

  def addConditions() = {
    val firstConditionMapping = mapping.cases.head
    val firstTemplateMapping = firstConditionMapping.mapping.asInstanceOf[TemplateMapping]

    //add first mapToClass
    val mapToClassPomUri = RMLUri(rmlModel.wikiTitle.resourceIri + "/ConditionalMapping" + "/" +
      firstConditionMapping.templateProperty + "_" + firstConditionMapping.operator + "_" + firstConditionMapping.value)
    val mapToClassPom = rmlModel.triplesMap.addConditionalPredicateObjectMap(mapToClassPomUri)

    mapToClassPom.addPredicate(RMLUri(RdfNamespace.RDF.namespace + "type"))
    mapToClassPom.addObject(RMLUri(firstTemplateMapping.mapToClass.uri))

    val conditionFunctionTermMap = addEqualCondition(firstConditionMapping, mapToClassPom)

    //add predicate object maps
    val rmlMapper = new RMLModelMapper(rmlModel)
    val state = new MappingState

    for(propertyMapping <- firstTemplateMapping.mappings)
    {
      val pomList = rmlMapper.addMapping(propertyMapping, state)
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

    val mapToClassPomUri = RMLUri(predicateObjectMap.resource.getURI).extend("/" + index)
    val mapToClassPom = predicateObjectMap.addFallbackMap(mapToClassPomUri)
    mapToClassPom.addPredicate(RMLUri(RdfNamespace.RDF.namespace + "type"))
    mapToClassPom.addObject(RMLUri(templateMapping.mapToClass.uri))

    val conditionFunctionTermMap = if(index < mapping.cases.size-1) {
       addEqualCondition(condition, mapToClassPom)
    } else null

    val state = new MappingState

    for(propertyMapping <- templateMapping.mappings)
    {
      val pomList = rmlMapper.addIndependentMapping(propertyMapping, state)
      for(pom <- pomList)
      {
        val condPom = rmlFactory.transformToConditional(pom)
        if(index < mapping.cases.size-1) {
          condPom.addEqualCondition(conditionFunctionTermMap)
        }
        mapToClassPom.addFallbackMap(condPom)
      }

    }

    addCondition(index + 1, mapToClassPom)

  }

  private def defineTriplesMap() =
  {
    rmlModel.triplesMap.addDCTermsType(new RMLLiteral("conditionalMapping"))
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addTemplate(rmlModel.rmlFactory.createRMLLiteral("http://en.dbpedia.org/resource/{wikititle}"))
    rmlModel.subjectMap.addIRITermType()
  }

  private def defineLogicalSource() =
  {
    val source = "https://" + rmlModel.wikiTitle.language.isoCode + ".wikipedia.org/wiki/{wikititle}"
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLLiteral(source))
  }


  def addDefaultMappings() =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    val state = new MappingState

    for(defaultMapping <- mapping.defaultMappings) {
      rmlMapper.addMapping(defaultMapping, state)
    }
  }

  /**
    * Adds the operator function to the mapping file
    *
    * @param conditionMapping
    * @param pom
    * @return
    */
  private def addEqualCondition(conditionMapping: ConditionMapping, pom: RMLConditionalPredicateObjectMap) : RMLFunctionTermMap =
  {

    val functionTermMapUri = pom.uri.extend("/FunctionTermMap")
    val functionTermMap = pom.addEqualCondition(functionTermMapUri)
    val functionValue = functionTermMap.addFunctionValue(functionTermMap.uri.extend("/FunctionValue"))
    functionValue.addLogicalSource(rmlModel.logicalSource)
    functionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val executePom = functionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/" + conditionMapping.operator))
    executePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObject(RMLUri(RdfNamespace.DBF.namespace + conditionMapping.operator))

    // checks if this condition needs a value or not
    if(conditionMapping.value != null) {
      val paramValuePom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/ValueParameterPOM"))
      paramValuePom.addPredicate(RMLUri(RdfNamespace.DBF.namespace  + conditionMapping.operator + "/" + DbfFunction.operatorFunction.valueParameter))
      paramValuePom.addObject(new RMLLiteral(conditionMapping.value))
    }

    // checks if this condition needs a property or not
    if(conditionMapping.templateProperty != null) {
      val paramPropertyPom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/PropertyParameterPOM"))
      paramPropertyPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + conditionMapping.operator + "/" + DbfFunction.operatorFunction.propertyParameter))
      paramPropertyPom.addObjectMap(paramPropertyPom.uri.extend("/ObjectMap")).addRMLReference(new RMLLiteral(conditionMapping.templateProperty))
    }

    functionTermMap
  }

}
