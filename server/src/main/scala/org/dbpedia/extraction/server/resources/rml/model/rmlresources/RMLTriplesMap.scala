package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents an RML Triples Map
  */
class RMLTriplesMap(override val resource: Resource) extends RMLResource(resource) {

  def addPredicateObjectMap(uri: RMLUri) : RMLPredicateObjectMap =
  {
    val predicateObjectMapResource = factory.createRMLPredicateObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), predicateObjectMapResource.resource)
    predicateObjectMapResource
  }

  def addLogicalSource(uri: RMLUri) : RMLLogicalSource =
  {
    val logicalSourceResource = factory.createRMLLogicalSource(uri)
    resource.addProperty(createProperty(RdfNamespace.RML.namespace + "logicalSource"), logicalSourceResource.resource)
    logicalSourceResource
  }

  def addLogicalSource(logicalSource: RMLLogicalSource) =
  {
    resource.addProperty(createProperty(RdfNamespace.RML.namespace + "logicalSource"), logicalSource.resource)
  }

  def addSubjectMap(uri: RMLUri) : RMLSubjectMap =
  {
    val subjectMapResource = factory.createRMLSubjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "subjectMap"), subjectMapResource.resource)
    subjectMapResource
  }





}
