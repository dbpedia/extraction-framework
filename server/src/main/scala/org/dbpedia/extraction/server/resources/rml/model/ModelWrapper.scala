package org.dbpedia.extraction.server.resources.rml.model

import java.io.StringWriter

import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, Statement}
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * Class that is a wrapper around an rdf model that adds behaviour (in this case Jena)
  */
class ModelWrapper() {

  val model: Model = ModelFactory.createDefaultModel()

  /**
    * Add a string as a property to a resource in this model
    */
  def addLiteralAsPropertyToResource(subject: Resource, predicate: String, literal: String): Unit =
  {
    subject.addProperty(model.createProperty(predicate), literal)
  }

  /**
    * Create a resource and add to this model
    */
  def addPropertyAsResource(subjectIRI: String, _object: String): Resource =
  {
    model.createResource(subjectIRI, model.createProperty(_object))
  }

  /**
    * Create a blank node and add to this model
    */
  def addBlankNode(): Resource =
  {
    model.createResource()
  }

  /**
    * Add a resource as a property to a resource in this model
    */
  def addResourceAsPropertyToResource(subject: Resource, predicate: String, _object: Resource): Unit =
  {
    subject.addProperty(model.createProperty(predicate), _object)
  }

  /**
    * Add a property as a property to a resource in this model
    */
  def addPropertyAsPropertyToResource(subject: Resource, predicate: String, _object: String): Unit =
  {
    subject.addProperty(model.createProperty(predicate), model.createProperty(_object))
  }

  /**
    * Returns resource using a String from this model
    */
  def getResource(resource: String) =
  {
    model.getResource(resource)
  }

  def printAsNTriples: Unit =
  {
    model.write(System.out, "N-TRIPLES")
  }

  def printAsTurtle: Unit =
  {
    model.write(System.out, "TURTLE")
  }

  def writeAsTurtle: String =
  {
    val out = new StringWriter()
    model.write(out, "TURTLE")
    out.toString
  }

}
