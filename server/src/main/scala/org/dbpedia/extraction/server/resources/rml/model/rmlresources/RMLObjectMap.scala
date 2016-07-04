package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an object map
  */
class RMLObjectMap(override val resource: Resource) extends RMLResource(resource) {

  def addRMLReference(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RML.namespace + "reference"), literal.toString())
  }

  def addParentTriplesMap(uri: RMLUri) : RMLTriplesMap =
  {
    val parentTriplesMap = factory.createRMLTriplesMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "parentTriplesMap"), parentTriplesMap.resource)
    parentTriplesMap
  }

  def addConstant(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RR.namespace + "constant"), literal.toString())
  }

}
