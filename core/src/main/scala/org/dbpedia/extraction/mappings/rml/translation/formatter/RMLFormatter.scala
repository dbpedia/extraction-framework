package org.dbpedia.extraction.mappings.rml.translation.formatter
import org.apache.jena.rdf.model.{Resource, Statement, StmtIterator}

import collection.JavaConverters._
import org.dbpedia.extraction.mappings.rml.translation.model.{ModelWrapper, RMLModel}
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 03.07.17.
  */
object RMLFormatter extends Formatter {

  private val offset = "\n\n"

  override def format(model: RMLModel, base : String): String = {

    try {
      val prefixes = getPrefixes(model.writeAsTurtle)
      val triplesMapPart = getTriplesMapPart(model, base)
      val subjectMapPart = getSubjectMapPart(model, base)
      val mappingsPart = getAllMappings(model, base)
      print(Seq(prefixes, triplesMapPart, subjectMapPart,mappingsPart).reduce((first, second) => first.concat('\n' + second)))
      triplesMapPart
    } catch {
      case x : Exception => x.printStackTrace(); null
    }





  }


  /**
    * Gets the main triples map of a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getTriplesMapPart(model : RMLModel, base : String) : String = {

    val freshModel = new ModelWrapper
    val triplesMapResource = model.triplesMap.resource
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(triplesMapResource.listProperties())
    val turtle = freshModel.writeAsTurtle(base)
    val formatted = turtle.replaceAll(",", ",\n\t\t\t\t\t\t\t  ")
    val heading = "### Main TriplesMap"

    heading + removePrefixes(formatted) + offset

  }

  /**
    * Gets the subjectMap of a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getSubjectMapPart(model : RMLModel, base : String) : String = {

    val freshModel = new ModelWrapper
    val subjectMapResource = model.subjectMap.resource
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(subjectMapResource.listProperties())
    val turtle = freshModel.writeAsTurtle(base)
    val formatted = turtle.replaceAll(",", ",\n\t\t\t\t\t")
    val heading = "### Main SubjectMap"

    heading + removePrefixes(formatted) + offset
  }

  /**
    * Gets all the property mappings from a mapping
    *
    * @param model
    * @param base
    * @return
    */
  private def getAllMappings(model: RMLModel, base : String) : String = {

    // Get the normal (non-conditional) mappings first
    val triplesMapResource = model.triplesMap.resource
    val statements = triplesMapResource.listProperties(model.model.createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")).toList

    statements.asScala.map(statement => {

      val predicateObjectMap = statement.getObject
      val properties = predicateObjectMap.asResource().listProperties()
      getMapping(properties, base)

    }).reduce((first, second) => first.concat("\n" + second)) // so return all concatenated mappings

  }

  /**
    * Retrieve all necessary constructs of a single mapping (predicate object map)
    *
    * @param properties
    * @param base
    * @return
    */
  private def getMapping(properties : StmtIterator, base : String) : String = {
    val propertiesArray: Array[Statement] = properties.toList.asScala.toArray
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(propertiesArray)
    val heading = "### Property Mapping"
    val predicateObjectMapString = removePrefixes(freshModel.writeAsTurtle(base : String))
    val functionTermMap = getObjectMap(propertiesArray.head.getSubject)
    val functionTermMapString = getFunctionTermMap(functionTermMap.listProperties(), base)
    heading + predicateObjectMapString + offset + functionTermMapString
  }

  /**
    * Retrieves all necessary constructs of a function term map
    *
    * @param properties
    * @param base
    * @return
    */
  private def getFunctionTermMap(properties : StmtIterator, base : String) : String = {
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(properties)

    val heading = "### Function Term Map"
    val functionTermMapString = removePrefixes(freshModel.writeAsTurtle(base : String))

    heading + functionTermMapString + offset

  }

  /**
    * Retrievs the object map resource of a resource
    *
    * @param resource
    * @return
    */
  private def getObjectMap(resource: Resource) : Resource = {
    val objectMapProperty = resource.getModel.getProperty(RdfNamespace.RR.namespace + "objectMap")
    resource.getProperty(objectMapProperty).getObject.asResource()
  }

  /**
    * Gets the prefixes from a turtle string
    *
    * @param turtle
    */
  private def getPrefixes(turtle : String) = {
    val result = turtle.split("\n")
                       .filter(line => line.contains("@"))
                       .reduce((first, second) => first.concat("\n" + second))
    result + offset
  }

  /**
    * Removes prefixes from a turtle string
    *
    * @param turtle
    * @return
    */
  private def removePrefixes(turtle : String) = {

    val result = turtle.split("\n")
        .filter(line => !line.contains("@prefix"))
        .filter(line => !line.contains("@base"))
        .reduce((first, second) => first.concat("\n" + second))

    result

  }
}
