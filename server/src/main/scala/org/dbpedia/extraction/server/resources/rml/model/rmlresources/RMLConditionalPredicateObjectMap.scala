package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents a conditional predicate object map
  *
  */
class RMLConditionalPredicateObjectMap(resource: Resource) extends RMLPredicateObjectMap(resource) {

  def addEqualCondition(uri: RMLUri) : RMLFunctionTermMap =
  {
    val functionTermMap = factory.createRMLFunctionTermMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "equalCondition"), functionTermMap.resource)
    functionTermMap
  }

  def addEqualCondition(functionTermMap: RMLFunctionTermMap) =
  {
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "equalCondition"), functionTermMap.resource)
  }

  def addTrueCondition(uri: RMLUri) : RMLFunctionTermMap =
  {
    val functionTermMap = factory.createRMLFunctionTermMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "trueCondition"), functionTermMap.resource)
    functionTermMap
  }

  def addFallbackMap(uri: RMLUri) : RMLConditionalObjectMap =
  {
    val conditionalObjectMap = factory.createRMLConditionalObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "fallbackMap"), conditionalObjectMap.resource)
    conditionalObjectMap
  }

}
