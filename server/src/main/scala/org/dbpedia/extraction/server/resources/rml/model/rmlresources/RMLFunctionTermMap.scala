package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents a function term map
  */
class RMLFunctionTermMap(resource: Resource) extends RMLObjectMap(resource) {

  def addFunctionValue(uri: RMLUri) : RMLTriplesMap =
  {
    val functionValue = factory.createRMLTriplesMap(uri)
    resource.addProperty(createProperty(RdfNamespace.FNML.namespace + "functionValue"), functionValue.resource)
    functionValue
  }

}
