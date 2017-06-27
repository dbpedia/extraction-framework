package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings.SimplePropertyMapping
import org.dbpedia.extraction.ontology.{OntologyClass, RdfNamespace}
import org.dbpedia.extraction.server.resources.rml.dbf.DbfFunction
import org.dbpedia.extraction.server.resources.rml.model.rmlresources._
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from SimplePropertyMappings and adds the triples to the given model
  */
class SimplePropertyRMLMapper(rmlModel: RMLModel, mapping: SimplePropertyMapping) {

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
    val uri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)
    val simplePropertyPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(uri)
    addSimplePropertyToPredicateObjectMap(simplePropertyPom)

    List(simplePropertyPom)
  }

  def addSimplePropertyMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] =
  {

    val simplePropertyMappingUri = new RMLUri(uri + "/SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)
    val simplePmPom = triplesMap.addPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addDCTermsType(new RMLLiteral("simplePropertyMapping"))
    simplePmPom.addPredicate(new RMLUri(mapping.ontologyProperty.uri))

    addSimplePropertyToPredicateObjectMap(simplePmPom)


    List(simplePmPom)

  }

  private def addDatatype(rMLPredicateObjectMap: RMLObjectMap) = {
    if (mapping.unit != null) {
      rMLPredicateObjectMap.addDatatype(new RMLUri(mapping.unit.uri))
    }
  }

  private def addSimplePropertyToPredicateObjectMap(simplePmPom: RMLPredicateObjectMap) =
  {

      val functionTermMapUri = simplePmPom.uri.extend("/FunctionTermMap")
      val functionTermMap = simplePmPom.addFunctionTermMap(functionTermMapUri)

      // adds the unit datatype if there is one
      addDatatype(functionTermMap)

      val functionValueUri = functionTermMapUri.extend("/FunctionValue")
      val functionValue = functionTermMap.addFunctionValue(functionValueUri)
      functionValue.addLogicalSource(rmlModel.logicalSource)
      functionValue.addSubjectMap(rmlModel.functionSubjectMap)

      val executePomUri = functionValueUri.extend("/ExecutePOM")
      val executePom = functionValue.addPredicateObjectMap(executePomUri)
      executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
      val ExecuteObjectMapUri = executePomUri.extend("/ObjectMap")
      executePom.addObjectMap(ExecuteObjectMapUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.simplePropertyFunction.name))

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
        addConstantParameterFunction("ontologyProperty", functionValue)
      }

  }

  private def addReferenceParameterFunction(param : String, functionValue: RMLTriplesMap) =
  {
    val parameterPomUri = functionValue.uri.extend("/" + param + "ParameterPOM")
    val parameterPom = functionValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + param + "Parameter"))
    val parameterObjectMapUri = parameterPomUri.extend("/ObjectMap")
    parameterPom.addObjectMap(parameterObjectMapUri).addRMLReference(new RMLLiteral(getParameterValue(param)))

  }

  private def addConstantParameterFunction(param : String, functionValue: RMLTriplesMap) =
  {
    val parameterPomUri = functionValue.uri.extend("/" + param + "ParameterPOM")
    val parameterPom = functionValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + param + "Parameter"))
    val parameterObjectMapUri = parameterPomUri.extend("/ObjectMap")
    parameterPom.addObjectMap(parameterObjectMapUri).addConstant(new RMLLiteral(getParameterValue(param)))
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


}
