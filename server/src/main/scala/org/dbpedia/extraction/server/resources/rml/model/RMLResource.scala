package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Model, Property, Resource}

/**
  * Represents a wrapper around a model resource
  */
class RMLResource(resource: Resource) {

  private val model = resource.getModel

  def addLiteral(predicate: String, literal: String): RMLResource = {
    resource.addProperty(createProperty(predicate), literal)
    this
  }

  def addUri(predicate: String, uri: String): RMLResource = {
    resource.addProperty(createProperty(predicate), createProperty(uri))
    this
  }

  def addRMLResource(predicate: String, rmlResource: RMLResource) : RMLResource = {
    resource.addProperty(createProperty(predicate), rmlResource.resource)
    this
  }

  private def createProperty(property: String) : Property = {
    model.createProperty(property)
  }

  private def createProperty(property: String, _type: String) : Property = {
    model.createProperty(property, _type)
  }

}
