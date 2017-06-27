package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings.ConstantMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.dbf.DbfFunction
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from Constant Mappings and adds the triples to the given model
  */
class ConstantRMLMapper(rmlModel: RMLModel, mapping: ConstantMapping) {

  def mapToModel() : List[RMLPredicateObjectMap] = {
    addConstantMapping()
  }

  def addConstantMapping() : List[RMLPredicateObjectMap] =
  {
    val uniqueUri = rmlModel.wikiTitle.resourceIri
    addConstantMappingToTriplesMap(uniqueUri, rmlModel.triplesMap)
  }

  def addIndependentConstantMapping(uri: String) : List[RMLPredicateObjectMap] =
  {
    val constantMappingUri = new RMLUri(uri + "/ConstantMapping/" + mapping.ontologyProperty.name + "/" + mapping.value)
    val constantPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(constantMappingUri)
    addConstantValuePredicateObjectMap(constantPom)
    List(constantPom)
  }

  def addConstantMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) : List[RMLPredicateObjectMap] = {
    val constantMappingUri = new RMLUri(uri + "/ConstantMapping/" + mapping.ontologyProperty.name + "/" + mapping.value)
    val constantPom = triplesMap.addPredicateObjectMap(constantMappingUri)
    addConstantValuePredicateObjectMap(constantPom)
    List(constantPom)
  }

  private def addConstantValuePredicateObjectMap(constantPom: RMLPredicateObjectMap) =
  {
    constantPom.addDCTermsType(new RMLLiteral("constantMapping"))
    constantPom.addPredicate(new RMLUri(mapping.ontologyProperty.uri))

    if(mapping.datatype == null) {
      val objectMapUri = constantPom.uri.extend("/ObjectMap")
      constantPom.addObjectMap(objectMapUri).addConstant(new RMLLiteral(mapping.value))
    } else {
      addUnitToPredicateObjectMap(constantPom)
    }

  }


  private def addUnitToPredicateObjectMap(constantPom: RMLPredicateObjectMap) =
  {
    val functionTermMapUri = constantPom.uri.extend("/FunctionTermMap")
    val functionTermMap = constantPom.addFunctionTermMap(functionTermMapUri)
    val functionValueUri = functionTermMapUri.extend("/FunctionValue")
    val functionValue = functionTermMap.addFunctionValue(functionValueUri)
    functionValue.addLogicalSource(rmlModel.logicalSource)
    functionValue.addSubjectMap(rmlModel.functionSubjectMap)

    val executePomUri = functionValueUri.extend("/ExecutePOM")
    val executePom = functionValue.addPredicateObjectMap(executePomUri)
    executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val ExecuteObjectMapUri = executePomUri.extend("/ObjectMap")
    executePom.addObjectMap(ExecuteObjectMapUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.unitFunction.name))

    addParameterFunction(DbfFunction.unitFunction.unitParameter, functionValue)
    addParameterFunction(DbfFunction.unitFunction.valueParameter, functionValue)

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
      case "unitParameter" => mapping.datatype.name
      case "valueParameter" => mapping.value
    }
  }

}
