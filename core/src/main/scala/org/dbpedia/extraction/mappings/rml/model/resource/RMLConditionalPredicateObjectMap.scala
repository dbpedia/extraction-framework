package org.dbpedia.extraction.mappings.rml.model.resource

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents a conditional predicate object map
  *
  */
class RMLConditionalPredicateObjectMap(resource: Resource) extends RMLPredicateObjectMap(resource) {

  lazy val equalCondition: RMLFunctionTermMap = getEqualCondition
  lazy val fallbacks: List[RMLPredicateObjectMap] = getFallbacks

  def addEqualCondition(uri: RMLUri): RMLFunctionTermMap = {
    val functionTermMap = factory.createRMLFunctionTermMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "equalCondition"), functionTermMap.resource)
    functionTermMap
  }

  def addEqualCondition(functionTermMap: RMLFunctionTermMap) = {
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "equalCondition"), functionTermMap.resource)
  }

  def addTrueCondition(uri: RMLUri): RMLFunctionTermMap = {
    val functionTermMap = factory.createRMLFunctionTermMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "trueCondition"), functionTermMap.resource)
    functionTermMap
  }

  def addFallbackMap(uri: RMLUri): RMLConditionalPredicateObjectMap = {
    val conditionalPredicateObjectMap = factory.createRMLConditionalPredicateObjectMap(uri)
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "fallbackMap"), conditionalPredicateObjectMap.resource)
    conditionalPredicateObjectMap
  }

  def addFallbackMap(conditionalPredicateObjectMap: RMLConditionalPredicateObjectMap) = {
    resource.addProperty(createProperty(RdfNamespace.CRML.namespace + "fallbackMap"), conditionalPredicateObjectMap.resource)
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def getEqualCondition: RMLFunctionTermMap = {

    if (resource.listProperties(createProperty(Property.EQUAL_CONDITION)).hasNext) {

      val stmnt = resource.listProperties(createProperty(Property.EQUAL_CONDITION)).nextStatement()
      RMLFunctionTermMap(stmnt.getObject.asResource())

    } else null
  }

  def getFallbacks: List[RMLPredicateObjectMap] = {

    var fallbacks = List[RMLPredicateObjectMap]()

    val iterator = resource.listProperties(createProperty(Property.FALLBACK_MAP))
    while (iterator.hasNext) {
      val stmnt = iterator.nextStatement()
      // if the pom is actually not a conditional pom, a normal pom will be returned
      val pom = RMLConditionalPredicateObjectMap(stmnt.getObject.asResource())
      fallbacks = fallbacks :+ pom
    }

    fallbacks

  }

}

object RMLConditionalPredicateObjectMap {

  /**
    * Creates an RMLConditionalPredicateObjectMap from a resource if it contains an crml:equalCondition
    *
    * @param resource
    * @return
    */
  def apply(resource: Resource): RMLPredicateObjectMap = {
    val properties = resource.listProperties().toList
    if (resource.hasProperty(resource.getModel.createProperty(Property.EQUAL_CONDITION))) {
      new RMLConditionalPredicateObjectMap(resource)
    } else {
      new RMLPredicateObjectMap(resource)
    }
  }

}
