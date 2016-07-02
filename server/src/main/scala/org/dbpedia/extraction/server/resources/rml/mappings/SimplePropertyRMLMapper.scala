package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{GeoCoordinatesMapping, SimplePropertyMapping}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.RMLTriplesMap
import org.dbpedia.extraction.server.resources.rml.model.{RMLModel, RMLResourceFactory}

/**
  * Creates RML Mapping from SimplePropertyMappings and adds the triples to the given model
  */
class SimplePropertyRMLMapper(rmlModel: RMLModel, mapping: SimplePropertyMapping) {

  private val rmlFactory = rmlModel.rmlFactory

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
    val simplePropertyMappingUri = rmlFactory.createRMLUri(uri + "/SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)
    val simplePmPom = triplesMap.addPredicateObjectMap(simplePropertyMappingUri)

    simplePmPom.addDCTermsType(rmlFactory.createRMLLiteral("simplePropertyMapping"))
    simplePmPom.addPredicate(rmlFactory.createRMLUri(mapping.ontologyProperty.uri))

    val objectMapUri = simplePropertyMappingUri.extend("/ObjectMap")
    val objectMap = simplePmPom.addObjectMap(objectMapUri)
    objectMap.addRMLReference(rmlFactory.createRMLLiteral(mapping.templateProperty))

    //add unit if present
//    if(mapping.unit != null) addUnitToPredicateObjectMap(predicateObjectMap, mapping.unit)

  }

  /**
    * Returns the base name + name added
    */
  private def baseName(name : String): String =
  {
    //"http://mappings.dbpedia.org/wiki/" + modelWrapper.wikiTitle.encodedWithNamespace + "/" + name
    ""
  }
/*
  private def addUnitToPredicateObjectMap(predicateObjectMap: Resource, unit : Datatype): Unit =
  {
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addPropertyAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", unit.uri)
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
  }
  */
}
