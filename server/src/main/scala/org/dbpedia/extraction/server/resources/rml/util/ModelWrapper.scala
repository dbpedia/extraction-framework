package org.dbpedia.extraction.server.resources.rml.util

import org.apache.jena.rdf.model.{Model, Resource}

/**
  * Class that is a wrapper around a Jena model with utility methods
  */
class ModelWrapper(model: Model) {



  /**
    * Add a string as a property to a resource in this model
    */
  def addLiteralPropertyToResource(subject: Resource, predicate: String, _object: String): Unit = {
    subject.addProperty(model.createProperty(predicate), _object)
  }

  /**
    * Create a resource and add to this model
    */
  def addPropertyResource(subjectIRI: String, _object: String): Resource = {
    model.createResource(subjectIRI, model.createProperty(_object))
  }

  /**
    * Create a blank node and add to this model
    */
  def addBlankNode(): Resource = {
    model.createResource()
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

  /**
    * Returns resource using a String from this model
    */
  def getResource(resource: String) = {
    model.getResource(resource)
  }

  def addPredicateObjectMapToResource(resource: Resource, predicate: String, _object: Resource) = {
    val predicateObjectMap = addBlankNode()
    addResourcePropertyToResource(resource, Prefixes("rr") + "predicateObjectMap", predicateObjectMap)
    addPropertyToResource(predicateObjectMap, Prefixes("rr") + "predicate", predicate)
    addResourcePropertyToResource(predicateObjectMap, Prefixes("rr") + "objectMap", _object)
    predicateObjectMap
  }

  def addPredicateObjectMapToRoot(predicate: String, _object: Resource): Resource = {
    addPredicateObjectMapToResource(getRoot, predicate, _object)
  }


  /**
    * Obtain root from model
    */
  def getRoot = {
    var root: Resource = null
    val it = model.listStatements()
    while(it.hasNext) {
      val next = it.next()
      if(next.getPredicate.getLocalName.equals("type")) {
        if(next.getObject.asResource().getLocalName.equals("triplesMap")) {
          root = next.getSubject.asResource()
        }
      }
    }
    root
  }

}
