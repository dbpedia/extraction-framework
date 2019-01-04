package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an RML Subject Map
  */
class RMLSubjectMap(override val resource: Resource) extends RMLResource(resource) {

  def addConstant(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RR.namespace + "constant"), literal.toString())
  }

  def addConstant(uri: RMLUri) = {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "constant"), createProperty(uri.toString()))
  }

  def addTermTypeIRI() = {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "termType"), createProperty(RdfNamespace.RR.namespace + "IRI"))
  }

  def addClass(uri: RMLUri) =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "class"), createProperty(uri.toString()))
  }

}
