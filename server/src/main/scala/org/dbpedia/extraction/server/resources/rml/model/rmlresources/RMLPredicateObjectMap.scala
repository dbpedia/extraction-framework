package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents a RML Predicate Object Map
  */
class RMLPredicateObjectMap(override val resource: Resource) extends RMLResource(resource) {

  def addPredicate(uri: RMLUri) =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicate"), createProperty(uri.toString()))
  }

  def addPredicate(literal: RMLLiteral) =
  {
    resource.addLiteral(createProperty(RdfNamespace.RR.namespace + "predicate"), literal)
  }

  def addObjectMap(uri: RMLUri) : RMLObjectMap =
  {
    val objectMap = factory.createRMLObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "objectMap"), objectMap.resource)
    objectMap
  }

  def addDCTermsType(literal: RMLLiteral) = {
    resource.addLiteral(createProperty(RdfNamespace.DCTERMS.namespace + "type"), literal.toString())
  }

}
