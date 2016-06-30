package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{GeoCoordinatesMapping, SimplePropertyMapping}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.server.resources.rml.model.RMLModelWrapper

/**
  * Creates RML Mapping from SimplePropertyMappings and adds the triples to the given model
  */
class SimplePropertyRMLMapper(modelWrapper: RMLModelWrapper, mapping: SimplePropertyMapping) {

  def mapToModel() = {
    addSimplePropertyMapping()
  }

  def addSimplePropertyMapping() =
  {
    val uniqueUri = baseName("")
    addSimplePropertyMappingToTriplesMap(uniqueUri, modelWrapper.triplesMap)
  }

  def addSimplePropertyMappingToTriplesMap(uri: String, triplesMap: Resource) =
  {

    //create predicate object map
    val predicateObjectMap = modelWrapper.addPredicateObjectMap(uri + "SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)

    //add dcterms type to predicate map
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.DCTERMS.namespace + "type", RdfNamespace.DBF.namespace + "simplePropertyMapping" )

    //add predicate to predicate object map
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "predicate", mapping.ontologyProperty.uri)

    //add object map with rml reference
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
    modelWrapper.addLiteralAsPropertyToResource(objectMap, RdfNamespace.RML.namespace + "reference", mapping.templateProperty)

    //add unit if present
    if(mapping.unit != null) addUnitToPredicateObjectMap(predicateObjectMap, mapping.unit)

    //add predicate object map to triples map
    modelWrapper.addPredicateObjectMapUriToTriplesMap(uri + "SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty, triplesMap)

  }

  /**
    * Returns the base name + name added
    */
  private def baseName(name : String): String =
  {
    "http://mappings.dbpedia.org/wiki/" + modelWrapper.wikiTitle.encodedWithNamespace + "/" + name
  }

  private def addUnitToPredicateObjectMap(predicateObjectMap: Resource, unit : Datatype): Unit =
  {
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addPropertyAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", unit.uri)
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
  }
}
