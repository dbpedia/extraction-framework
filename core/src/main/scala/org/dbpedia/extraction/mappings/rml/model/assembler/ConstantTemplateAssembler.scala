package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.ConstantTemplate
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.ontology.{OntologyObjectProperty, RdfNamespace}

/**
  * Created by wmaroy on 25.07.17.
  */
class ConstantTemplateAssembler(rmlModel : RMLModel, language : String, template: ConstantTemplate, counter : Int) {

  private lazy val constantCount = counter + 1

  def assemble() = {
    val uniqueUri = rmlModel.triplesMap.resource.getURI
    addConstantMappingToTriplesMap(uniqueUri, rmlModel.triplesMap)
  }

  def addConstantMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap)  =
  {
    val constantMappingUri = new RMLUri(uri + "/ConstantMapping/" + constantCount)

    val constantPom = triplesMap.addPredicateObjectMap(constantMappingUri)
    addConstantValuePredicateObjectMap(constantPom)
    List(constantPom)
  }

  private def addConstantValuePredicateObjectMap(constantPom: RMLPredicateObjectMap) =
  {
    constantPom.addPredicate(new RMLUri(template.ontologyProperty.uri))

    if(template.unit == null) {

      if(template.ontologyProperty.isInstanceOf[OntologyObjectProperty]) {
        constantPom.addObject(new RMLUri(template.value))
      } else {
        constantPom.addObject(new RMLLiteral(template.value))
      }
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
      case "unitParameter" => template.unit.name
      case "valueParameter" => template.value
    }
  }


}
