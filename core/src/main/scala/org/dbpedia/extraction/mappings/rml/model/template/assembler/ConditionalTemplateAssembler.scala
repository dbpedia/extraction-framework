package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.{ConditionMapping, TemplateMapping}
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.translate.mapper.{MappingState, RMLModelMapper}
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 27.07.17.
  *
  * This is a very complicated class due to the workflow. Firstly the workflow of how conditional mappings are generated
  * must be understood very thoroughly.
  *
  */
class ConditionalTemplateAssembler(rmlModel: RMLModel, baseUri: String, conditionalTemplate: ConditionalTemplate, language: String, counter : Counter) {

  private val conditionalBaseUri = baseUri + "/ConditionalMapping" + "/"+ counter.conditionals

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def assemble() : List[RMLPredicateObjectMap] = {
    addConditions()
    List()
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def addConditions() = {

    def addConditions(resource : RMLResource, template : ConditionalTemplate, counter : Counter): Unit = {

      ////////////////////////////////////////////////////////////////////////////
      // Creates condition FTM and update the counter
      ////////////////////////////////////////////////////////////////////////////

      val condition = conditionalTemplate.condition
      val tuple = if(condition != null || !condition.operator.equals(Condition.OTHERWISE)) {
        createEqualCondition(condition, conditionalBaseUri, counter)
      } else {
        (counter, null)
      }
      val conditionFunctionTermMap = tuple._2
      val updatedCounter = tuple._1.update(simpleProperties = 0, geoCoordinates = 0, startDates = 0, endDates = 0)

      ////////////////////////////////////////////////////////////////////////////
      // Creates poms from all subtemplates and adds them as fallbacks
      ////////////////////////////////////////////////////////////////////////////

      //add predicate object maps
      val subTemplates = conditionalTemplate.templates

      // foldLeft: assembles all templates from head to tail,
      // every assembly returns a tuple (state) which is in turn passed through to the next assembly
      val finalState = subTemplates.foldLeft((updatedCounter, List[RMLPredicateObjectMap]()))((state, template) => {

        val updatedState = TemplateAssembler.assembleTemplate(rmlModel, conditionalBaseUri + "/Condition/" + counter.subConditions, template, language, state._1)
        addFallbacksToResource(resource, updatedState)

        // returns the current counter and adds the RMLPredicateObjectMaps to the list
        (updatedState._1, state._2 ++ updatedState._2)

      })

      val finalPomList = finalState._2

      ////////////////////////////////////////////////////////////////////////////
      // Check if template has a class mapping, if so, the poms of the fallback
      // will be set onto the pom of the class mapping. If not, the poms of the
      // fallbacks will be put onto the first pom from the sub templates
      ////////////////////////////////////////////////////////////////////////////

      val updatedResource : RMLPredicateObjectMap = if(template.hasClass) {

        // a mapping can only contain one class mapping, or one set of conditional class mappings
        if(containsClassMapping) throw new IllegalArgumentException("Class is already mapped. Only one class mapping is allowed.")

        val mapToClassPom = createMapToClassPom(template, counter)
        addFallbackToResource(resource, mapToClassPom)
        mapToClassPom

      } else {
        val firstTemplatePom = finalPomList.head
        firstTemplatePom
      }

      ////////////////////////////////////////////////////////////////////////////
      // Check if the resource is a TriplesMap or a PredicateObjectMap
      // If it is a TM: add the condition FTM to every POM (including 'updatedResource')
      // If is is a POM: add the condition FTM only to 'updatedResource'
      // Only execute this if there is a condition ..
      ////////////////////////////////////////////////////////////////////////////

      if(conditionFunctionTermMap != null) addConditionFTMToResource(resource, conditionFunctionTermMap, updatedResource, finalPomList)
      val finalCounter = finalState._1.update(subConditions = finalState._1.subConditions + 1)

      //continue recursively
      if(template.hasFallback) {
        addConditions(updatedResource, template.fallback, finalCounter)
      }

    }

    addConditions(rmlModel.triplesMap, conditionalTemplate, counter)

  }

  private def createMapToClassPom(template : ConditionalTemplate, counter : Counter) : RMLConditionalPredicateObjectMap = {
    val mapToClassPomUri = RMLUri(conditionalBaseUri + "/Condition/" + counter.subConditions + "/ClassMapping")
    val mapToClassPom = rmlModel.rmlFactory.createRMLConditionalPredicateObjectMap(mapToClassPomUri)
    mapToClassPom.addPredicate(RMLUri(RdfNamespace.RDF.namespace + "type"))
    mapToClassPom.addObject(RMLUri(template.ontologyClass.uri))
    mapToClassPom
  }

  private def addConditionFTMToResource(resource : RMLResource, condFTM : RMLFunctionTermMap, updatedResource : RMLPredicateObjectMap, pomList : List[RMLPredicateObjectMap]) : Unit = {
    resource match {
      case triplesMap: RMLTriplesMap => {
        pomList.foreach(pom => {
          val condPom = rmlModel.rmlFactory.transformToConditional(pom)
          condPom.addEqualCondition(condFTM)
        })
        val condPom = rmlModel.rmlFactory.transformToConditional(updatedResource)
        condPom.addEqualCondition(condFTM)
      }
      case pom : RMLConditionalPredicateObjectMap => {
        val condPom = rmlModel.rmlFactory.transformToConditional(updatedResource)
        condPom.addEqualCondition(condFTM)
      }
    }
  }

  private def addFallbackToResource(resource : RMLResource, condPom : RMLConditionalPredicateObjectMap): Unit = {
    resource match {
      case triplesMap: RMLTriplesMap => triplesMap.addConditionalPredicateObjectMap(condPom)
      case pom : RMLConditionalPredicateObjectMap => pom.addFallbackMap(condPom)
    }
  }

  private def addFallbacksToResource(resource :RMLResource, state : (Counter, List[RMLPredicateObjectMap])) : Unit  = {
    // foreach: every pom in pomList is transformed to a conditional pom with an added condition
    val pomList = state._2
    pomList.foreach(pom => {
      val condPom = rmlModel.rmlFactory.transformToConditional(pom)
      addFallbackToResource(resource, condPom)
    })
  }

  /**
    * Adds the operator function to the mapping file
    * Increments the counter's subconditions by 1
    *
    * @return
    */
  private def createEqualCondition(condition: Condition, conditionalBaseURI : String, counter : Counter) : (Counter, RMLFunctionTermMap) =
  {

    val functionTermMapUri = RMLUri(conditionalBaseURI + "/Condition/" + counter.subConditions + "/FunctionTermMap")
    val functionTermMap = rmlModel.rmlFactory.createRMLFunctionTermMap(functionTermMapUri)
    val functionValue = functionTermMap.addFunctionValue(functionTermMap.uri.extend("/FunctionValue"))
    functionValue.addLogicalSource(rmlModel.logicalSource)
    functionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val executePom = functionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/" + condition.operator))
    executePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObject(RMLUri(RdfNamespace.DBF.namespace + condition.operator))

    def addValueParameter(value : String, operator : String) = {
      val paramValuePom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/ValueParameterPOM"))
      paramValuePom.addPredicate(RMLUri(RdfNamespace.DBF.namespace  + operator + "/" + DbfFunction.operatorFunction.valueParameter))
      paramValuePom.addObject(new RMLLiteral(value))
    }

    def addPropertyParameter(property : String, operator : String) = {
      val paramPropertyPom = functionValue.addPredicateObjectMap(functionValue.uri.extend("/PropertyParameterPOM"))
      paramPropertyPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + operator + "/" + DbfFunction.operatorFunction.propertyParameter))
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

    (counter, functionTermMap)
  }

  private def containsClassMapping : Boolean = {

    val subjectMapHasClass = rmlModel.subjectMap.resource.hasProperty(rmlModel.model.createProperty(RdfNamespace.RR.namespace + "class"))
    val pomHasType = rmlModel.triplesMap.predicateObjectMaps.exists(pom => pom.predicatePropertyURI.equals(RdfNamespace.RDF.namespace + "type"))

    subjectMapHasClass || pomHasType
  }

}
