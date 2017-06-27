package org.dbpedia.extraction.server.resources.rml.mappings

import com.sun.org.apache.xerces.internal.util.URI
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.{OntologyClass, RdfNamespace}
import org.dbpedia.extraction.server.resources.rml.dbf.DbfFunction
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources._

import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from ConditionalMappings and adds the triples to the given model
  *
  */
class ConditionalRMLMapper(rmlModel: RMLModel, mapping: ConditionalMapping) {

  private val rmlMapper = new RMLModelMapper(rmlModel)
  private val rmlFactory = rmlModel.rmlFactory

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
    mapToClassPom.addDCTermsType(new RMLLiteral("conditionalMapping"))

    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    val mapToClassOmUri = mapToClassPomUri.extend("/ObjectMap")
    val mapToClassOm = mapToClassPom.addObjectMap(mapToClassOmUri)
    mapToClassOm.addConstant(new RMLUri(firstTemplateMapping.mapToClass.uri))

    // adds the related classes of this condition
    addRelatedClassesToOM(mapToClassOm, firstTemplateMapping.mapToClass)

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

    val mapToClassPomUri = new RMLUri(predicateObjectMap.resource.getURI).extend("/" + index)
    val mapToClassPom = predicateObjectMap.addFallbackMap(mapToClassPomUri)
    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    val objectMap = mapToClassPom.addObjectMap(mapToClassPom.uri.extend("/ObjectMap")).addConstant(new RMLUri(templateMapping.mapToClass.uri))

    // adds all the related classes for this condition
    addRelatedClassesToOM(objectMap, templateMapping.mapToClass)

    val conditionFunctionTermMap = addEqualCondition(condition, mapToClassPom)
    val state = new MappingState

    for(propertyMapping <- templateMapping.mappings)
    {
      val pomList = rmlMapper.addIndependentMapping(propertyMapping, state)
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
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
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

    val executePom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/ExecutePOM"))
    executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObjectMap(executePom.uri.extend("/ObjectMap")).addConstant(new RMLUri(RdfNamespace.DBF.namespace + conditionMapping.operator))

    // checks if this condition needs a value or not
    if(conditionMapping.value != null) {
      val paramValuePom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/ValueParameterPOM"))
      paramValuePom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace  + conditionMapping.operator + "/" + DbfFunction.operatorFunction.valueParameter))
      paramValuePom.addObjectMap(paramValuePom.uri.extend("/ObjectMap")).addConstant(new RMLLiteral(conditionMapping.value))
    }

    // checks if this condition needs a property or not
    if(conditionMapping.templateProperty != null) {
      val paramPropertyPom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/PropertyParameterPOM"))
      paramPropertyPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + conditionMapping.operator + "/" + DbfFunction.operatorFunction.propertyParameter))
      paramPropertyPom.addObjectMap(paramPropertyPom.uri.extend("/ObjectMap")).addRMLReference(new RMLLiteral(conditionMapping.templateProperty))
    }

    functionTermMap
  }

  /**
    * Add related classes to the subject map
    * @param subjectMap
    */
  private def addExtraClassesToSubjectMap(subjectMap: RMLSubjectMap) =
  {
    val relatedClasses = mapping.cases.head.mapping.asInstanceOf[TemplateMapping].mapToClass.relatedClasses
    for(cls <- relatedClasses) {
      subjectMap.addClass(new RMLUri(cls.uri))
    }
  }

  /**
    * Adds related classes of an ontology class to a pom
    * @param om
    * @param mapToClass
    */
  private def addRelatedClassesToOM(om : RMLObjectMap, mapToClass : OntologyClass) =
  {
    mapToClass.relatedClasses.foreach(relatedClass => {
      om.addConstant(new RMLUri(relatedClass.uri))
    })
  }

}
