package org.dbpedia.extraction.server.resources.rml.mappings

import org.dbpedia.extraction.mappings.DateIntervalMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.dbf.DbfFunction
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import scala.language.reflectiveCalls

/**
  * Creates RML Mapping from DateIntervalMappings and adds the triples to the given model
  */
class DateIntervalRMLMapper(rmlModel: RMLModel, mapping: DateIntervalMapping) {

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
    val startUri = new RMLUri(uri + "/StartDate/" + mapping.startDateOntologyProperty.name + "/" + mapping.endDateOntologyProperty.name + "/" + mapping.templateProperty)
    val startPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(startUri)
    val endUri = new RMLUri(uri + "/EndDate/" + mapping.startDateOntologyProperty.name + "/" + mapping.endDateOntologyProperty.name + "/" + mapping.templateProperty)
    val endPom = rmlModel.rmlFactory.createRMLPredicateObjectMap(endUri)

    addDateIntervalMappingToPredicateObjectMaps(startPom, endPom)

    List(startPom, endPom)
  }

  def addDateIntervalMappingToTriplesMap(uri: String, triplesMap : RMLTriplesMap) : List[RMLPredicateObjectMap] = {

    val startUri = new RMLUri(uri + "/StartDate/" + mapping.startDateOntologyProperty.name + "/" + mapping.endDateOntologyProperty.name + "/" + mapping.templateProperty)
    val startDateIntervalPom = triplesMap.addPredicateObjectMap(startUri)


    val endUri = new RMLUri(uri + "/EndDate/" + mapping.startDateOntologyProperty.name + "/" + mapping.endDateOntologyProperty.name + "/" + mapping.templateProperty)
    val endDateIntervalPom = triplesMap.addPredicateObjectMap(endUri)

    addDateIntervalMappingToPredicateObjectMaps(startDateIntervalPom, endDateIntervalPom)

    List(startDateIntervalPom, endDateIntervalPom)

  }

  private def addDateIntervalMappingToPredicateObjectMaps(startDateIntervalPom: RMLPredicateObjectMap, endDateIntervalPom: RMLPredicateObjectMap) =
  {
    startDateIntervalPom.addDCTermsType(new RMLLiteral("startDateIntervalMapping"))
    startDateIntervalPom.addPredicate(new RMLUri(mapping.startDateOntologyProperty.uri))

    endDateIntervalPom.addDCTermsType(new RMLLiteral("endDateIntervalMapping"))
    endDateIntervalPom.addPredicate(new RMLUri(mapping.endDateOntologyProperty.uri))

    startDateIntervalPom.addDBFEndDate(endDateIntervalPom)
    endDateIntervalPom.addDBFStartDate(startDateIntervalPom)


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
    val startExecutePomUri = startFunctionValueUri.extend("/ExecutePOM")
    val startExecutePom = startFunctionValue.addPredicateObjectMap(startExecutePomUri)
    startExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val startExecuteObjectMapUri = startExecutePomUri.extend("/ObjectMap")
    startExecutePom.addObjectMap(startExecuteObjectMapUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.startDateFunction.name))

    // adding the property parameter pom of the start date function
    val startParameterPomUri = startFunctionValueUri.extend("/PropertyParameterPOM")
    val startParameterPom = startFunctionValue.addPredicateObjectMap(startParameterPomUri)
    startParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.startDateFunction.startDateParameter))
    val startParameterObjectMapUri = startParameterPomUri.extend("/ObjectMap")
    startParameterPom.addObjectMap(startParameterObjectMapUri).addRMLReference(new RMLLiteral(mapping.templateProperty))

    // adding the start date ontology property parameter of the start date function
    val startOntologyParameterPomUri = startFunctionValueUri.extend("/OntologyParameterPOM")
    val startOntologyParameterPom = startFunctionValue.addPredicateObjectMap(startOntologyParameterPomUri)
    startOntologyParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.startDateFunction.ontologyParameter))
    val startOntologyParameterObjectMapUri = startOntologyParameterPomUri.extend("/ObjectMap")
    startOntologyParameterPom.addObjectMap(startOntologyParameterObjectMapUri).addConstant(new RMLLiteral(mapping.startDateOntologyProperty.name))

    endFunctionValue.addLogicalSource(rmlModel.logicalSource)
    endFunctionValue.addSubjectMap(rmlModel.functionSubjectMap)

    // adding the execute pom of the end date function
    val endExecutePomUri = endFunctionValueUri.extend("/ExecutePOM")
    val endExecutePom = endFunctionValue.addPredicateObjectMap(endExecutePomUri)
    endExecutePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    val endExecuteObjectMapUri = endExecutePomUri.extend("/ObjectMap")
    endExecutePom.addObjectMap(endExecuteObjectMapUri).addConstant(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.endDateFunction.name))

    // adding the property parameter pom of the end date function
    val endParameterPomUri = endFunctionValueUri.extend("/PropertyParameterPOM")
    val endParameterPom = endFunctionValue.addPredicateObjectMap(endParameterPomUri)
    endParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace +  DbfFunction.endDateFunction.endDateParameter))
    val endParameterObjectMapUri = endParameterPomUri.extend("/ObjectMap")
    endParameterPom.addObjectMap(endParameterObjectMapUri).addRMLReference(new RMLLiteral(mapping.templateProperty))

    // adding the end date ontology property parameter of the end date function
    val endOntologyParameterPomUri = endFunctionValueUri.extend("/OntologyParameterPOM")
    val endOntologyParameterPom = endFunctionValue.addPredicateObjectMap(endOntologyParameterPomUri)
    endOntologyParameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + DbfFunction.endDateFunction.ontologyParameter))
    val endOntologyParameterObjectMapUri = endOntologyParameterPomUri.extend("/ObjectMap")
    endOntologyParameterPom.addObjectMap(endOntologyParameterObjectMapUri).addConstant(new RMLLiteral(mapping.endDateOntologyProperty.name))

  }


}
