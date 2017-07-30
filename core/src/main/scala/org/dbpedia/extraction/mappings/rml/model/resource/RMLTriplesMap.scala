package org.dbpedia.extraction.mappings.rml.model.resource

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.ontology.RdfNamespace

import scala.collection.JavaConverters._

/**
  * Represents an RML Triples Map
  */
class RMLTriplesMap(override val resource: Resource) extends RMLResource(resource) {

  lazy val predicateObjectMaps : List[RMLPredicateObjectMap] = getPredicateObjectMaps
  lazy val subjectMap : RMLSubjectMap = getSubjectMap

  def addPredicateObjectMap(uri: RMLUri) : RMLPredicateObjectMap =
  {
    val predicateObjectMapResource = factory.createRMLPredicateObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), predicateObjectMapResource.resource)
    predicateObjectMapResource
  }

  def addPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap) =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), predicateObjectMap.resource)
  }

  def addConditionalPredicateObjectMap(uri: RMLUri) : RMLConditionalPredicateObjectMap =
  {
    val condPom = factory.createRMLConditionalPredicateObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), condPom.resource)
    condPom
  }

  def addConditionalPredicateObjectMap(predicateObjectMap: RMLConditionalPredicateObjectMap) : RMLConditionalPredicateObjectMap =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap"), predicateObjectMap.resource)
    predicateObjectMap
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

  def addSubjectMap(subjectMap: RMLSubjectMap) =
  {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "subjectMap"), subjectMap.resource)
  }

  def addDCTermsType(literal: RMLLiteral) = {
    //resource.addLiteral(createProperty(RdfNamespace.DCTERMS.namespace + "type"), literal.toString())
  }

  def addLanguage(language : String) = {
    resource.addProperty(createProperty(RdfNamespace.RR.namespace + "language"), language)
  }

  private def getPredicateObjectMaps: List[RMLPredicateObjectMap] =
  {
    val properties = resource.listProperties(createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")).toList
    properties.asScala.map(property => new RMLPredicateObjectMap(property.getObject.asResource())).toList
  }

  private def getSubjectMap : RMLSubjectMap = {
    val property = resource.listProperties(createProperty(Property.SUBJECTMAP))
    if(property.hasNext) {
      val stmnt = property.nextStatement()
      val subjectMapResource = stmnt.getObject.asResource()
      RMLSubjectMap(subjectMapResource)
    } else null
  }


}

object RMLTriplesMap {

  def apply(resource : Resource) = {
    new RMLTriplesMap(resource)
  }

}
