package org.dbpedia.extraction.mappings.rml.translate.mapper

import org.dbpedia.extraction.mappings.SimplePropertyMapping
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.model.{RMLModel, RMLTranslationModel}
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.ontology.RdfNamespace

import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from SimplePropertyMappings and adds the triples to the given model
  */
class SimplePropertyRMLMapper(rmlModel: RMLTranslationModel, mapping: SimplePropertyMapping) {

  val language = mapping.language.isoCode

  def mapToModel() : List[RMLPredicateObjectMap] = {
    addSimplePropertyMapping()
  }

  def addSimplePropertyMapping() : List[RMLPredicateObjectMap] =
  {
    val uniqueUri = rmlModel.wikiTitle.resourceIri
    addSimplePropertyMappingToTriplesMap(uniqueUri, rmlModel.triplesMap)
  }

  def addIndependentSimplePropertyMapper() : List[RMLPredicateObjectMap] =
  {
    val uri = RMLUri(rmlModel.wikiTitle.resourceIri + "/SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)
    val simplePropertyPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(uri)
    addSimplePropertyToPredicateObjectMap(simplePropertyPom)

    List(simplePropertyPom)
  }

  def addSimplePropertyMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val simplePropertyMappingUri = RMLUri(uri + "/SimplePropertyMapping/" + TemplateRMLMapper.simplePropertyCount)
    TemplateRMLMapper.increaseSimplePropertyCount() // this is needed to assign a number in the uri's
    val simplePmPom = triplesMap.addPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addDCTermsType(new RMLLiteral("simplePropertyMapping"))
    simplePmPom.addPredicate(RMLUri(mapping.ontologyProperty.uri))
    addSimplePropertyToPredicateObjectMap(simplePmPom)

    List(simplePmPom)

  }

  private def addDatatype(objectMap: RMLObjectMap) = {
    if (mapping.unit != null) {
      objectMap.addDatatype(RMLUri(mapping.unit.uri))
    }
  }

  private def addLanguage(objectMap : RMLObjectMap) = {
    if (mapping.ontologyProperty.range.name.equals("rdf:langString")) objectMap.addLanguage(language)
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
      case "factor" => mapping.factor.toString
      case "transform" => mapping.transform
      case "select" => mapping.select
      case "prefix" => mapping.prefix
      case "suffix" => mapping.suffix
      case "unit" => mapping.unit.name
      case "property" => mapping.templateProperty
      case "ontologyProperty" => mapping.ontologyProperty.name
    }

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

  /**
    * Adds all the parameter POMs if neccessary
    *
    * @param functionValue
    * @return
    */
  private def addParameters(functionValue: RMLTriplesMap) = {
    addReferenceParameterFunction("property", functionValue)

    if(mapping.factor != 1) {
      addConstantParameterFunction("factor", functionValue)
    }

    if(mapping.transform != null) {
      addConstantParameterFunction("transform", functionValue)
    }

    if(mapping.select != null) {
      addConstantParameterFunction("select", functionValue)
    }

    if(mapping.prefix != null) {
      addConstantParameterFunction("prefix", functionValue)
    }

    if(mapping.suffix != null) {
      addConstantParameterFunction("suffix", functionValue)
    }

    if(mapping.unit != null) {
      addConstantParameterFunction("unit", functionValue)
    }

    if(mapping.ontologyProperty != null) {
      //addConstantParameterFunction("ontologyProperty", functionValue) // will be added by inferencing
    }
  }

}
