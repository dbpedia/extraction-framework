package org.dbpedia.extraction.mappings.rml.model.resource

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Represents a function term map
  */
class RMLFunctionTermMap(resource: Resource) extends RMLObjectMap(resource) {

  lazy val functionValue = getFunctionValue

  def addFunctionValue(uri: RMLUri) : RMLTriplesMap =
  {
    val functionValue = factory.createRMLTriplesMap(uri)
    resource.addProperty(createProperty(RdfNamespace.FNML.namespace + "functionValue"), functionValue.resource)
    functionValue
  }

  def getFunction : Function = {

    // retrieve all objects and tuple them by reference and constant
    val objects = functionValue.predicateObjectMaps.map(pom => {
      if(pom.hasReferenceObjectMap) {
        ("references", pom.predicatePropertyURI -> pom.objectMap.reference.toString())
      } else {
        ("constants", pom.predicatePropertyURI -> pom.rrObject)
      }
    })

    // group the tuples by ._1 and convert the resulting ._2 list to a map
    val grouped = objects.groupBy(_._1).mapValues(_.map(_._2))
    val map = grouped.map(entry => {
      entry._1 -> entry._2.toMap
    })

    // create the function
    val name = map("constants")(Property.EXECUTES)
    val constants = map("constants") - Property.EXECUTES
    val references = map("references")

    val function = Function(name, references, constants)
    function
  }

  def getFunctionValue : RMLTriplesMap = {
    val stmnt = resource.listProperties(createProperty(Property.FUNCTIONVALUE)).nextStatement()
    val fvResource = stmnt.getObject.asResource()
    RMLTriplesMap(fvResource)
  }


}

case class Function(name : String, references : Map[String, String], constants : Map[String, String])

object RMLFunctionTermMap {

  def apply(resource: Resource): RMLFunctionTermMap = {
    new RMLFunctionTermMap(resource)
  }

}
