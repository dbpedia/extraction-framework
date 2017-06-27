package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an RML Subject Map
  */
class RMLSubjectMap(override val resource: Resource) extends RMLResource(resource) {

  def addConstant(literal: RMLLiteral) : RMLSubjectMap =
  {
    resource.addLiteral(createProperty(RdfNamespace.RR.namespace + "constant"), literal.toString())
    this
  }

  def addRMLReference(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RML.namespace + "reference"), literal.toString())
  }

  def addConstant(uri: RMLUri) : RMLSubjectMap = {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "constant"), createProperty(uri.toString()))
    this
  }

  def addClass(uri: RMLUri) : RMLSubjectMap =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "class"), createProperty(uri.toString()))
    this
  }

  def addIRITermType() : RMLSubjectMap =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "termType"), createProperty(RdfNamespace.RR.namespace + "IRI"))
    this
  }

  def addLiteralTermType() : RMLSubjectMap =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "termType"), createProperty(RdfNamespace.RR.namespace + "Literal"))
    this
  }

  def addBlankNodeTermType() : RMLSubjectMap =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "termType"), createProperty(RdfNamespace.RR.namespace + "BlankNode"))
    this
  }

  def addTemplate(literal : RMLLiteral) : RMLSubjectMap = {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "template"), literal.toString())
    this
  }

}
