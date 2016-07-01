package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an object map
  */
class RMLObjectMap(val resource: Resource) extends RMLResource(resource) {

  def addRMLReference(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RML.namespace + "reference"), literal.toString())
  }

}
