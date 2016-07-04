package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Model, ModelFactory, Property}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.rmlresources._

/**
  * Factory for creating all types of RMLResources
  */
class RMLResourceFactory(model: Model) {

  def createRMLResource(uri: RMLUri) : RMLResource =
  {
    new RMLResource(model.createResource(uri.toString()))
  }

  def createRMLTriplesMap(uri: RMLUri) : RMLTriplesMap =
  {
    new RMLTriplesMap(model.createResource(uri.toString(), createProperty(RdfNamespace.RR.namespace + "TriplesMap")))
  }

  def createRMLLogicalSource(uri: RMLUri) : RMLLogicalSource =
  {
    new RMLLogicalSource(model.createResource(uri.toString(), createProperty(RdfNamespace.RML.namespace + "LogicalSource")))
  }

  def createRMLSubjectMap(uri: RMLUri) : RMLSubjectMap =
  {
    new RMLSubjectMap(model.createResource(uri.toString(), createProperty(RdfNamespace.RML.namespace + "SubjectMap")))
  }

  def createRMLPredicateObjectMap(uri: RMLUri) : RMLPredicateObjectMap =
  {
    new RMLPredicateObjectMap(model.createResource(uri.toString(), createProperty(RdfNamespace.RR.namespace + "PredicateObjectMap")))
  }

  def createRMLObjectMap(uri: RMLUri) : RMLObjectMap =
  {
    new RMLObjectMap(model.createResource(uri.toString(), createProperty(RdfNamespace.RR.namespace + "ObjectMap")))
  }

  def createRMLConditionalObjectMap(uri: RMLUri) : RMLConditionalObjectMap =
  {
    new RMLConditionalObjectMap(model.createResource(uri.toString(), createProperty(RdfNamespace.RR.namespace + "ObjectMap")))
  }

  def createRMLFunctionTermMap(uri: RMLUri) : RMLFunctionTermMap =
  {
    new RMLFunctionTermMap(model.createResource(uri.toString(), createProperty(RdfNamespace.FNML.namespace + "FunctionTermMap")))
  }

  def createRMLUri(uri: String) : RMLUri =
  {
    new RMLUri(uri)
  }

  def createRMLLiteral(literal: String) : RMLLiteral =
  {
    new RMLLiteral(literal)
  }

  private def createProperty(s: String) : Property =
  {
    model.createProperty(s)
  }

}
