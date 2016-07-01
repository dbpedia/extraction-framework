package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an RML Triples Map
  */
class RMLTriplesMap(val resource: Resource) extends RMLResource(resource) {

  def addRMLPredicateObjectMap(uri: RMLUri) : RMLPredicateObjectMap = {

    val predicateObjectMapResource = factory.createRMLPredicateObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), predicateObjectMapResource.resource)

    predicateObjectMapResource
  }



}
