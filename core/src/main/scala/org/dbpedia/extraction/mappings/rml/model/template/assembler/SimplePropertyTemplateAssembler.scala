package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.rml.model.AbstractRMLModel
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 24.07.17.
  *
  */
class SimplePropertyTemplateAssembler(rmlModel : AbstractRMLModel, baseUri: String, language : String, template: SimplePropertyTemplate, counter : Counter) {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def assemble(independent : Boolean = false) : List[RMLPredicateObjectMap] = {
    if(!independent) {
      val triplesMap = rmlModel.triplesMap
      addSimplePropertyMappingToTriplesMap(baseUri, triplesMap)
    } else {
      addIndependentSimplePropertyMappingToTriplesMap(baseUri)
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def addIndependentSimplePropertyMappingToTriplesMap(uri: String) : List[RMLPredicateObjectMap] =
  {

    val simplePropertyMappingUri = RMLUri(uri + "/SimplePropertyMapping/" + counter.simpleProperties)
    val simplePmPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addPredicate(RMLUri(template.ontologyProperty.uri))
    addSimplePropertyToPredicateObjectMap(simplePmPom)

    List(simplePmPom)

  }

  private def addSimplePropertyMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val simplePropertyMappingUri = RMLUri(uri + "/SimplePropertyMapping/" + counter.simpleProperties)
    val simplePmPom = triplesMap.addPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addPredicate(RMLUri(template.ontologyProperty.uri))
    addSimplePropertyToPredicateObjectMap(simplePmPom)

    List(simplePmPom)

  }


  private def addSimplePropertyToPredicateObjectMap(simplePmPom: RMLPredicateObjectMap) =
  {

    val functionTermMapUri = simplePmPom.uri.extend("/FunctionTermMap")
    val functionTermMap = simplePmPom.addFunctionTermMap(functionTermMapUri)

    // adds the unit datatype if there is one
    addDatatype(functionTermMap)

    // adds rr:language if possible
    addLanguage(functionTermMap)

    // adds the function value for simplepropertymapping
    val functionValueUri = functionTermMapUri.extend("/FunctionValue")
    val functionValue = functionTermMap.addFunctionValue(functionValueUri)
    functionValue.addLogicalSource(rmlModel.logicalSource)
    functionValue.addSubjectMap(rmlModel.functionSubjectMap)

    // the next few lines check if the SimplePropertyFunction already exists or not in the mapping file so that
    // there is always a maximum of one ExecutePOMs of this function in a mapping
    if(!rmlModel.containsResource(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.simplePropertyFunction.name)))
    {
      createSimplePropertyFunction(functionValue)
    }
    else
    {
      functionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/SimplePropertyFunction"))
    }

    // add the remaining parameters
    addParameters(functionValue)

  }

  /**
    * Adds an Execute Predicate Object Map (fixed uri) for the SimplePropertyFunction to a given FunctionValue
    *
    * @param functionValue
    * @return
    */
  private def createSimplePropertyFunction(functionValue : RMLTriplesMap) = {
    val executePomUri = RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/SimplePropertyFunction") // to make the uri short and simple
    val executePom = functionValue.addPredicateObjectMap(executePomUri)
    executePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.simplePropertyFunction.name))
  }

  private def addDatatype(objectMap: RMLObjectMap) = {
    if (template.unit != null) {
      objectMap.addDatatype(RMLUri(template.unit.uri))
    }


  }

  /**
    * Adds a rr:language if necessary
    *
    * @param objectMap
    * @return
    */
  private def addLanguage(objectMap : RMLObjectMap) = {
    if (template.ontologyProperty.range.name.equals("rdf:langString")) objectMap.addLanguage(language)
  }


  /**
    * Adds all the parameter POMs if necessary
    *
    * @param functionValue
    * @return
    */
  private def addParameters(functionValue: RMLTriplesMap) = {
    addReferenceParameterFunction("property", functionValue)

    if(template.factor.toDouble != 1) {
      addConstantParameterFunction("factor", functionValue)
    }

    if(template.transform != null) {
      addConstantParameterFunction("transform", functionValue)
    }

    if(template.select != null) {
      addConstantParameterFunction("select", functionValue)
    }

    if(template.prefix != null) {
      addConstantParameterFunction("prefix", functionValue)
    }

    if(template.suffix != null) {
      addConstantParameterFunction("suffix", functionValue)
    }

    if(template.unit != null) {
      addConstantParameterFunction("unit", functionValue)
    }

    if(template.ontologyProperty != null) {
      //addConstantParameterFunction("ontologyProperty", functionValue) // will be added by inferencing
    }
  }

  private def addReferenceParameterFunction(param : String, functionValue: RMLTriplesMap) =
  {
    val parameterPomUri = functionValue.uri.extend("/" + param + "ParameterPOM")
    val parameterPom = functionValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + param + "Parameter"))
    val parameterObjectMapUri = parameterPomUri.extend("/ObjectMap")
    val objectMap = parameterPom.addObjectMap(parameterObjectMapUri)
    objectMap.addRMLReference(new RMLLiteral(getParameterValue(param)))
  }

  private def addConstantParameterFunction(param : String, functionValue: RMLTriplesMap) =
  {
    val parameterPomUri = functionValue.uri.extend("/" + param + "ParameterPOM")
    val parameterPom = functionValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + param + "Parameter"))
    parameterPom.addObject(new RMLLiteral(getParameterValue(param)))
  }

  private def getParameterValue(param: String) : String =
  {

    param match {
      case "factor" => template.factor.toString
      case "transform" => template.transform
      case "select" => template.select
      case "prefix" => template.prefix
      case "suffix" => template.suffix
      case "unit" => template.unit.name
      case "property" => template.property
      case "ontologyProperty" => template.ontologyProperty.name
    }

  }

}
