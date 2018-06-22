package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.rml.model.AbstractRMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.EndDateTemplate
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.translate.dbf.DbfFunction
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 26.07.17.
  */
class EndDateTemplateAssembler(rmlModel: AbstractRMLModel, baseUri: String, language: String, template: EndDateTemplate, counter: Counter, independent: Boolean) {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def assemble(): List[RMLPredicateObjectMap] = {
    addEndDateMapping()
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def addEndDateMapping(): List[RMLPredicateObjectMap] = {
    addEndDateMappingToTriplesMap(baseUri, rmlModel.triplesMap)
  }

  def addEndDateMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap): List[RMLPredicateObjectMap] = {

    val endUri = RMLUri(uri + "/" + RMLUri.ENDDATEMAPPING + "/" + counter.endDates)
    val endDateIntervalPom = if (!independent) {
      triplesMap.addPredicateObjectMap(endUri)
    } else {
      rmlModel.rmlFactory.createRMLPredicateObjectMap(endUri)
    }

    addEndDateMappingToPredicateObjectMaps(endDateIntervalPom)

    List(endDateIntervalPom)

  }

  private def addEndDateMappingToPredicateObjectMaps(endDateIntervalPom: RMLPredicateObjectMap) = {
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
