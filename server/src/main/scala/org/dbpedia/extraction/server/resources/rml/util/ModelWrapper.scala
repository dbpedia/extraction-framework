package org.dbpedia.extraction.server.resources.rml.util

import org.apache.jena.rdf.model.{Model, Resource}

/**
  * Class that is a wrapper around a Jena model with utility methods
  */
class ModelWrapper(model: Model) {

  /**
    * Add a string as a property to a resource in this model
    */
  def addStringPropertyToResource(subject: Resource, predicate: String, _object: String): Unit = {
    subject.addProperty(model.createProperty(predicate), _object)
  }

  /**
    * Create a resource and add to this model
    */
  def addPropertyResource(subjectIRI: String, _object: String): Resource = {
    model.createResource(subjectIRI, model.createProperty(_object))
  }

  /**
    * Create a resource and add to this model
    */
  def addPropertyResource(subjectIRI: String): Resource = {
    model.createResource(subjectIRI)
  }

  /**
    * Add a resource as a property to a resource in this model
    */
  def addResourcePropertyToResource(subject: Resource, predicate: String, _object: Resource): Unit = {
    subject.addProperty(model.createProperty(predicate), _object)
  }

  /**
    * Add a property as a property to a resource in this model
    */
  def addPropertyToResource(subject: Resource, predicate: String, _object: String): Unit = {
    subject.addProperty(model.createProperty(predicate), model.createProperty(_object))
  }

}
