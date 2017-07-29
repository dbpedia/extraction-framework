package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.StartDateTemplate
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.translate.mapper.TemplateRMLMapper
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 26.07.17.
  */
class StartDateTemplateAssembler(rmlModel : RMLModel, baseUri : String, language : String, template: StartDateTemplate, counter : Counter) {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def assemble() : List[RMLPredicateObjectMap] = {
    addStartDateMapping()
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def addStartDateMapping() : List[RMLPredicateObjectMap]  =
  {
    addStartDateMappingToTriplesMap(baseUri, rmlModel.triplesMap)
  }

  private def addStartDateMappingToTriplesMap(uri: String, triplesMap : RMLTriplesMap) : List[RMLPredicateObjectMap] = {

    val startUri = RMLUri(uri + "/" +  RMLUri.STARTDATEMAPPING + "/" + counter.startDates)
    val startDateIntervalPom = triplesMap.addPredicateObjectMap(startUri)

    addStartDateMappingToPredicateObjectMaps(startDateIntervalPom)

    List(startDateIntervalPom)

  }

  private def addStartDateMappingToPredicateObjectMaps(startDateIntervalPom: RMLPredicateObjectMap) =
  {
    startDateIntervalPom.addPredicate(RMLUri(template.ontologyProperty.uri))

    val startFunctionTermMapUri = startDateIntervalPom.uri.extend("/FunctionTermMap")
    val startFunctionTermMap = startDateIntervalPom.addFunctionTermMap(startFunctionTermMapUri)

    val startFunctionValueUri = startFunctionTermMapUri.extend("/FunctionValue")
    val startFunctionValue = startFunctionTermMap.addFunctionValue(startFunctionValueUri)

    startFunctionValue.addLogicalSource(rmlModel.logicalSource)
    startFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    // adding the execute pom of the start date function
    val startExecutePom = startFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/StartDateFunction"))
    startExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    startExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.startDateFunction.name))

    // adding the property parameter pom of the start date function
    val startParameterPomUri = startFunctionValueUri.extend("/PropertyParameterPOM")
    val startParameterPom = startFunctionValue.addPredicateObjectMap(startParameterPomUri)
    startParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.startDateFunction.startDateParameter))
    val startParameterObjectMapUri = startParameterPomUri.extend("/ObjectMap")
    startParameterPom.addObjectMap(startParameterObjectMapUri).addRMLReference(new RMLLiteral(template.property))

  }

}
