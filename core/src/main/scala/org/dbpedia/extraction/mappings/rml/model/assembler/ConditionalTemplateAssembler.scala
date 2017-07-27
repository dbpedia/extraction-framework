package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.{ConditionMapping, TemplateMapping}
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLConditionalPredicateObjectMap, RMLFunctionTermMap, RMLLiteral, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.translate.mapper.{MappingState, RMLModelMapper}
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 27.07.17.
  */
class ConditionalTemplateAssembler(rmlModel: RMLModel, conditionalTemplate: ConditionalTemplate, language: String, counter : Int) {

  def assemble() = {

  }
  /**

  def addConditions() = {
    val firstConditionMapping = conditionalTemplate.condition

    //add first mapToClass
    val mapToClassPomUri = new RMLUri(rmlModel.triplesMap.resource.getURI + "/ConditionalMapping" + "/" + counter)
    val mapToClassPom = rmlModel.triplesMap.addConditionalPredicateObjectMap(mapToClassPomUri)

    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    mapToClassPom.addObject(new RMLUri(conditionalTemplate.ontologyClass.uri))

    val conditionFunctionTermMap = addEqualCondition(firstConditionMapping, mapToClassPom)

    //add predicate object maps
    val state = new MappingState

    for(propertyMapping <- conditionalTemplate.templates)
    {
      val pomList = List()//rmlMapper.addMapping(propertyMapping, state)

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

    //if(index == mapping.cases.size) {
    //  return
    //}

    val condition = mapping.cases(index)
    val templateMapping = condition.mapping.asInstanceOf[TemplateMapping]

    val mapToClassPomUri = new RMLUri(predicateObjectMap.resource.getURI).extend("/" + index)
    val mapToClassPom = predicateObjectMap.addFallbackMap(mapToClassPomUri)
    mapToClassPom.addPredicate(new RMLUri(RdfNamespace.RDF.namespace + "type"))
    mapToClassPom.addObject(new RMLUri(templateMapping.mapToClass.uri))

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

  /**
    * Adds the operator function to the mapping file
    *
    * @param pom
    * @return
    */
  private def addEqualCondition(condition: Condition, pom: RMLConditionalPredicateObjectMap) : RMLFunctionTermMap =
  {

    val functionTermMapUri = pom.uri.extend("/FunctionTermMap")
    val functionTermMap = pom.addEqualCondition(functionTermMapUri)
    val functionValue = functionTermMap.addFunctionValue(functionTermMap.uri.extend("/FunctionValue"))
    functionValue.addLogicalSource(rmlModel.logicalSource)
    functionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val executePom = functionValue.addPredicateObjectMap(new RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/" + condition.operator))
    executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObject(new RMLUri(RdfNamespace.DBF.namespace + condition.operator))

    def addValueParameter(value : String, operator : String) = {
      val paramValuePom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/ValueParameterPOM"))
      paramValuePom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace  + operator + "/" + DbfFunction.operatorFunction.valueParameter))
      paramValuePom.addObject(new RMLLiteral(value))
    }

    def addPropertyParameter(property : String, operator : String) = {
      val paramPropertyPom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/PropertyParameterPOM"))
      paramPropertyPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + operator + "/" + DbfFunction.operatorFunction.propertyParameter))
      paramPropertyPom.addObjectMap(paramPropertyPom.uri.extend("/ObjectMap")).addRMLReference(new RMLLiteral(property))
    }

    condition.operator match {
      case Condition.ISSET => {
        val conditionInstance = condition.asInstanceOf[IsSetCondition]
        addPropertyParameter(conditionInstance.property, conditionInstance.operator)
      }
      case Condition.EQUALS => {
        val conditionInstance = condition.asInstanceOf[EqualsCondition]
        addPropertyParameter(conditionInstance.property, conditionInstance.operator)
        addValueParameter(conditionInstance.value, conditionInstance.operator)
      }
      case Condition.CONTAINS => {
        val conditionInstance = condition.asInstanceOf[EqualsCondition]
        addPropertyParameter(conditionInstance.property, conditionInstance.operator)
        addValueParameter(conditionInstance.value, conditionInstance.operator)
      }
      case Condition.OTHERWISE => {
        val conditionInstance = condition.asInstanceOf[EqualsCondition]
        addPropertyParameter(conditionInstance.property, conditionInstance.operator)
        addValueParameter(conditionInstance.value, conditionInstance.operator)
      }
    }

    functionTermMap
  }

    **/

}
