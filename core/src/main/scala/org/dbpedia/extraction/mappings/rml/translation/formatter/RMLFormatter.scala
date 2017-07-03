package org.dbpedia.extraction.mappings.rml.translation.formatter
import org.apache.jena.rdf.model.StmtIterator

import collection.JavaConverters._
import org.dbpedia.extraction.mappings.rml.translation.model.{ModelWrapper, RMLModel}
import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 03.07.17.
  */
object RMLFormatter extends Formatter {

  private val offset = "\n\n"

  override def format(model: RMLModel, base : String): String = {

    val prefixes = getPrefixes(model.writeAsTurtle)
    val triplesMapPart = getTriplesMapPart(model, base)
    val subjectMapPart = getSubjectMapPart(model, base)
    val mappingsPart = getAllMappings(model, base)

    print(Seq(prefixes, triplesMapPart, subjectMapPart,mappingsPart).reduce((first, second) => first.concat('\n' + second)))

    triplesMapPart
  }


  /**
    * Gets the main triples map of a mapping
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
    }).reduce((first, second) => first.concat("\n" + second))

  }

  /**
    * Retrieve all necessary constructs of a single mapping (predicate object map)
    * @param properties
    * @param base
    * @return
    */
  private def getMapping(properties : StmtIterator, base : String) : String ={
    val freshModel = new ModelWrapper
    freshModel.insertRDFNamespacePrefixes()
    freshModel.model.add(properties)
    val heading = "### Property Mapping"
    val predicateObjectMapString = removePrefixes(freshModel.writeAsTurtle(base : String))
    heading + predicateObjectMapString + offset
  }

  /**
    * Gets the prefixes from a turtle string
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
