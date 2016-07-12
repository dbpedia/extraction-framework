package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{GeoCoordinatesMapping, SimplePropertyMapping}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

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

  private def addSimplePropertyToPredicateObjectMap(simplePmPom: RMLPredicateObjectMap) =
  {
    val executeFunction = mapping.factor != 1 ||
      mapping.select != null || mapping.prefix != null ||
      mapping.suffix != null || mapping.transform != null ||
      mapping.unit != null

    if (!executeFunction) {
      val objectMapUri = simplePmPom.uri.extend("/ObjectMap")
      val objectMap = simplePmPom.addObjectMap(objectMapUri)
      objectMap.addRMLReference(new RMLLiteral(mapping.templateProperty))
    }
    else {

      val functionTermMapUri = simplePmPom.uri.extend("/FunctionTermMap")
      val functionTermMap = simplePmPom.addFunctionTermMap(functionTermMapUri)
      val functionValueUri = functionTermMapUri.extend("/FunctionValue")
      val functionValue = functionTermMap.addFunctionValue(functionValueUri)
      functionValue.addLogicalSource(rmlModel.logicalSource)
      functionValue.addSubjectMap(rmlModel.functionSubjectMap)

      val executePomUri = functionValueUri.extend("/ExecutePOM")
      val executePom = functionValue.addPredicateObjectMap(executePomUri)
      executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
      val ExecuteObjectMapUri = executePomUri.extend("/ObjectMap")
      executePom.addObjectMap(ExecuteObjectMapUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + "simplePropertyFunction"))

      addParameterFunction("property", functionValue)

      if(mapping.factor != 1) {
        addParameterFunction("factor", functionValue)
      }

      if(mapping.transform != null) {
        addParameterFunction("transform", functionValue)
      }

      if(mapping.select != null) {
        addParameterFunction("select", functionValue)
      }

      if(mapping.prefix != null) {
        addParameterFunction("prefix", functionValue)
      }

      if(mapping.suffix != null) {
        addParameterFunction("suffix", functionValue)
      }

      if(mapping.unit != null) {
        addParameterFunction("unit", functionValue)
      }
    }

  }

  private def addParameterFunction(param : String, functionValue: RMLTriplesMap) =
  {
    val parameterPomUri = functionValue.uri.extend("/" + param + "ParameterPOM")
    val parameterPom = functionValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + param + "Parameter"))
    val parameterObjectMapUri = parameterPomUri.extend("/ObjectMap")
    parameterPom.addObjectMap(parameterObjectMapUri).addRMLReference(new RMLLiteral(getParameterValue(param)))

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
    }
  }


}
