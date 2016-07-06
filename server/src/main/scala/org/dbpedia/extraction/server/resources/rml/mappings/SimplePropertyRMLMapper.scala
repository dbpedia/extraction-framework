package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{GeoCoordinatesMapping, SimplePropertyMapping}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.server.resources.rml.model.{RMLModel, RMLResourceFactory}

/**
  * Creates RML Mapping from SimplePropertyMappings and adds the triples to the given model
  */
class SimplePropertyRMLMapper(rmlModel: RMLModel, mapping: SimplePropertyMapping) {

  def mapToModel() = {
    addSimplePropertyMapping()
  }

  def addSimplePropertyMapping() =
  {
    val uniqueUri = rmlModel.wikiTitle.resourceIri
    addSimplePropertyMappingToTriplesMap(uniqueUri, rmlModel.triplesMap)
  }

  def addSimplePropertyMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {
    val simplePropertyMappingUri = new RMLUri(uri + "/SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)
    val simplePmPom = triplesMap.addPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addDCTermsType(new RMLLiteral("simplePropertyMapping"))
    simplePmPom.addPredicate(new RMLUri(mapping.ontologyProperty.uri))

    val objectMapUri = simplePropertyMappingUri.extend("/ObjectMap")
    val objectMap = simplePmPom.addConditionalMap(objectMapUri)
    objectMap.addRMLReference(new RMLLiteral(mapping.templateProperty))
    objectMap.addIRITermType()

    val trueCondFunTermMapUri = objectMapUri.extend("/FunTermMap")
    val trueCondFunTermMap = objectMap.addEqualCondition(trueCondFunTermMapUri)

    val trueCondFunValueUri = trueCondFunTermMapUri.extend("/FunValue")
    val trueCondFunValue = trueCondFunTermMap.addFunctionValue(trueCondFunValueUri)
    trueCondFunValue.addLogicalSource(rmlModel.logicalSource)
    trueCondFunValue.addSubjectMap(rmlModel.functionSubjectMap)

    val executePomUri = trueCondFunValueUri.extend("/ExecutePOM")
    val executePom = trueCondFunValue.addPredicateObjectMap(executePomUri)
    executePom.addPredicate(new RMLUri(RdfNamespace.FNO.namespace + "executes"))
    executePom.addObjectMap(executePomUri.extend("/ObjectMap"))
      .addConstant(new RMLUri(RdfNamespace.DBF.namespace + "startsWith"))

    val parameterPomUri = trueCondFunValueUri.extend("/ParameterPom1")
    val parameterPom = trueCondFunValue.addPredicateObjectMap(parameterPomUri)
    parameterPom.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "startsWithParameter1"))
    parameterPom.addObjectMap(parameterPomUri.extend("/ObjectMap")).addRMLReference(new RMLLiteral("http://en.dbpedia.org/resource"))

    val parameterPomUri2 = trueCondFunValueUri.extend("/ParameterPom2")
    val parameterPom2 = trueCondFunValue.addPredicateObjectMap(parameterPomUri2)
    parameterPom2.addPredicate(new RMLUri(RdfNamespace.DBF.namespace + "startsWithParameter2"))
    parameterPom2.addObjectMap(parameterPomUri2.extend("/ObjectMap")).addRMLReference(new RMLLiteral(mapping.templateProperty))

    val fallbackMapUri = objectMapUri.extend("/FallBackMap")
    val fallBackMap = objectMap.addFallbackMap(fallbackMapUri)
    fallBackMap.addRMLReference(new RMLLiteral(mapping.templateProperty))


    //add unit if present
    if(mapping.unit != null) addUnitToPredicateObjectMap(simplePmPom, mapping.unit)

    //TODO other parameters

  }


  private def addUnitToPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap, unit : Datatype): Unit =
  {
    //TODO
  }
}
