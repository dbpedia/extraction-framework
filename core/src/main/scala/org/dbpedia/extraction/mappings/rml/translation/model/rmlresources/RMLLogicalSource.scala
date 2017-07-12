package org.dbpedia.extraction.mappings.rml.translation.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an RML logical source
  */
class RMLLogicalSource(override val resource: Resource) extends RMLResource(resource){

  def addReferenceFormulation(uri: RMLUri) =
  {
    resource.addProperty(createProperty(RdfNamespace.RML.namespace + "referenceFormulation"), createProperty(uri.toString()))
  }

  def addIterator(literal: RMLLiteral) =
  {
    resource.addProperty(createProperty(RdfNamespace.RML.namespace+ "iterator"), literal.toString())
  }

  def addSource(uri: RMLUri) =
  {
    resource.addProperty(createProperty(RdfNamespace.RML.namespace + "source"), uri.toString())
  }

  def addSource(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RML.namespace + "source"), literal.toString())
  }

}
