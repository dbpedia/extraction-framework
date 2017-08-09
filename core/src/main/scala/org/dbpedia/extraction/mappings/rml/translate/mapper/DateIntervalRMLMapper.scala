package org.dbpedia.extraction.mappings.rml.translate.mapper

import org.dbpedia.extraction.mappings.DateIntervalMapping
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.mappings.rml.model.{AbstractRMLModel, RMLTranslationModel}
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.ontology.RdfNamespace

import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from DateIntervalMappings and adds the triples to the given model
  */
class DateIntervalRMLMapper(rmlModel: RMLTranslationModel, mapping: DateIntervalMapping) {

  //TODO refactor

  def mapToModel() : List[RMLPredicateObjectMap] = {
    addDateIntervalMapping()
  }

  def addDateIntervalMapping() : List[RMLPredicateObjectMap]  =
  {
    val uri = rmlModel.wikiTitle.resourceIri
    addDateIntervalMappingToTriplesMap(uri, rmlModel.triplesMap)
  }

  def addIndependentDateIntervalMapping() : List[RMLPredicateObjectMap] = {
    val uri = rmlModel.wikiTitle.resourceIri
    val startUri = RMLUri(uri + "/StartDateMapping/" + TemplateRMLMapper.startDateCount)
    val startPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(startUri)
    TemplateRMLMapper.increaseStartDateCount()

    val endUri = RMLUri(uri + "/EndDateMapping/" + TemplateRMLMapper.endDateCount)
    val endPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(endUri)
    TemplateRMLMapper.increaseEndDateCount()

    addDateIntervalMappingToPredicateObjectMaps(startPom, endPom)

    List(startPom, endPom)
  }

  def addDateIntervalMappingToTriplesMap(uri: String, triplesMap : RMLTriplesMap) : List[RMLPredicateObjectMap] = {

    val startUri = RMLUri(uri + "/StartDateMapping/" + TemplateRMLMapper.startDateCount)
    val startDateIntervalPom = triplesMap.addPredicateObjectMap(startUri)
    TemplateRMLMapper.increaseStartDateCount()

    val endUri = RMLUri(uri + "/EndDateMapping/" + TemplateRMLMapper.endDateCount)
    val endDateIntervalPom = triplesMap.addPredicateObjectMap(endUri)
    TemplateRMLMapper.increaseEndDateCount()

    addDateIntervalMappingToPredicateObjectMaps(startDateIntervalPom, endDateIntervalPom)

    List(startDateIntervalPom, endDateIntervalPom)

  }

  private def addDateIntervalMappingToPredicateObjectMaps(startDateIntervalPom: RMLPredicateObjectMap, endDateIntervalPom: RMLPredicateObjectMap) =
  {
    startDateIntervalPom.addPredicate(RMLUri(mapping.startDateOntologyProperty.uri))
    endDateIntervalPom.addPredicate(RMLUri(mapping.endDateOntologyProperty.uri))

    val startFunctionTermMapUri = startDateIntervalPom.uri.extend("/FunctionTermMap")
    val startFunctionTermMap = startDateIntervalPom.addFunctionTermMap(startFunctionTermMapUri)

    val endFunctionTermMapUri = endDateIntervalPom.uri.extend("/FunctionTermMap")
    val endFunctionTermMap = endDateIntervalPom.addFunctionTermMap(endFunctionTermMapUri)

    val startFunctionValueUri = startFunctionTermMapUri.extend("/FunctionValue")
    val startFunctionValue = startFunctionTermMap.addFunctionValue(startFunctionValueUri)

    val endFunctionValueUri = endFunctionTermMapUri.extend("/FunctionValue")
    val endFunctionValue = endFunctionTermMap.addFunctionValue(endFunctionValueUri)

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
    startParameterPom.addObjectMap(startParameterObjectMapUri).addRMLReference(new RMLLiteral(mapping.templateProperty))

    endFunctionValue.addLogicalSource(rmlModel.logicalSource)
    endFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    // adding the execute pom of the end date function
    val endExecutePom = endFunctionValue.addPredicateObjectMap(RMLUri(rmlModel.triplesMap.resource.getURI + "/Function/EndDateFunction"))
    endExecutePom.addPredicate(RMLUri(RdfNamespace.FNO.namespace + "executes"))
    endExecutePom.addObject(RMLUri(RdfNamespace.DBF.namespace + DbfFunction.endDateFunction.name))

    // adding the property parameter pom of the end date function
    val endParameterPomUri = endFunctionValueUri.extend("/PropertyParameterPOM")
    val endParameterPom = endFunctionValue.addPredicateObjectMap(endParameterPomUri)
    endParameterPom.addPredicate(RMLUri(RdfNamespace.DBF.namespace +  DbfFunction.endDateFunction.endDateParameter))
    val endParameterObjectMapUri = endParameterPomUri.extend("/ObjectMap")
    endParameterPom.addObjectMap(endParameterObjectMapUri).addRMLReference(new RMLLiteral(mapping.templateProperty))

  }


}
