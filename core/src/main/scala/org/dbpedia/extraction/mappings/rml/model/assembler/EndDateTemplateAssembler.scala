package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, StartDateTemplate}
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 26.07.17.
  */
class EndDateTemplateAssembler(rmlModel : RMLModel, baseUri : String, language : String, template: EndDateTemplate, counter : Int) {

  def assemble() = {
    addEndDateMapping()
  }

  def addEndDateMapping()  =
  {
    addEndDateMappingToTriplesMap(baseUri, rmlModel.triplesMap)
  }

  def addEndDateMappingToTriplesMap(uri: String, triplesMap : RMLTriplesMap) = {

    val endUri = RMLUri(uri + "/" +  RMLUri.ENDDATEMAPPING + "/" + counter)
    val endDateIntervalPom = triplesMap.addPredicateObjectMap(endUri)

    addEndDateMappingToPredicateObjectMaps(endDateIntervalPom)

  }

  private def addEndDateMappingToPredicateObjectMaps(endDateIntervalPom: RMLPredicateObjectMap) =
  {
    endDateIntervalPom.addPredicate(RMLUri(template.ontologyProperty.uri))

    val endFunctionTermMapUri = endDateIntervalPom.uri.extend("/FunctionTermMap")
    val endFunctionTermMap = endDateIntervalPom.addFunctionTermMap(endFunctionTermMapUri)

    val endFunctionValueUri = endFunctionTermMapUri.extend("/FunctionValue")
    val endFunctionValue = endFunctionTermMap.addFunctionValue(endFunctionValueUri)

    endFunctionValue.addLogicalSource(rmlModel.logicalSource)
    endFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    // adding the execute pom of the end date function
    val endExecutePom = endFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/EndDateFunction"))
    endExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    endExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.endDateFunction.name))

    // adding the property parameter pom of the end date function
    val endParameterPomUri = endFunctionValueUri.extend("/PropertyParameterPOM")
    val endParameterPom = endFunctionValue.addPredicateObjectMap(endParameterPomUri)
    endParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.endDateFunction.endDateParameter))
    val endParameterObjectMapUri = endParameterPomUri.extend("/ObjectMap")
    endParameterPom.addObjectMap(endParameterObjectMapUri).addRMLReference(new RMLLiteral(template.property))

  }

}
