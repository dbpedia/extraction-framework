package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.DateIntervalMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

/**
  * Creates RML Mapping from DateIntervalMappings and adds the triples to the given model
  */
class DateIntervalRMLMapper(modelWrapper: RMLModel, mapping: DateIntervalMapping) {
  /**

  def mapToModel() = {
    addDateIntervalMapping()
  }

  def addDateIntervalMapping() =
  {
    val uniqueUri= baseName("")
    addDateIntervalMappingToTriplesMap(uniqueUri, modelWrapper.triplesMap)
  }

  def addDateIntervalMappingToTriplesMap(uri: String, triplesMap : Resource) =
  {

    //create predicate object map for date
    val uniqueUri = uri + "dateInterval/" + mapping.startDateOntologyProperty.name + "/" + mapping.startDateOntologyProperty.name + "/" + mapping.endDateOntologyProperty.name
    val dateIntervalPom = modelWrapper.addPredicateObjectMap(uniqueUri)
    modelWrapper.addResourceAsPropertyToResource(triplesMap, RdfNamespace.RR.namespace + "predicateObjectMap", dateIntervalPom)

    //add dcterms:type to predicate
    modelWrapper.addPropertyAsPropertyToResource(dateIntervalPom, RdfNamespace.DCTERMS.namespace + "type", RdfNamespace.DBF.namespace + "dateIntervalMapping")

    //add predicate to start date pom
    modelWrapper.addPropertyAsPropertyToResource(dateIntervalPom, RdfNamespace.RR.namespace + "predicate", RdfNamespace.DBF.namespace + "something")

    //add object map to start date pom
    val objectMapStartString= uniqueUri + "/" + "IntervalFunctionMap"
    val objectMapStart = modelWrapper.addResourceWithPredicate(objectMapStartString, RdfNamespace.FNML.namespace + "FunctionTermMap")
    modelWrapper.addResourceAsPropertyToResource(dateIntervalPom, RdfNamespace.RR.namespace + "objectMap", objectMapStart)

    //add triples map to object map
    val triplesMapStartString = objectMapStartString + "/TriplesMap"
    val triplesMapStart = modelWrapper.addTriplesMap(triplesMapStartString)
    modelWrapper.addResourceAsPropertyToResource(objectMapStart, RdfNamespace.FNML.namespace + "functionValue", triplesMapStart)

    //add logical source to triples map
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RML.namespace + "logicalSource", modelWrapper.logicalSource)

    /**
      * add subject map
      */

    //add subject map to triples map
    val subjectMapStartString = triplesMapStartString + "/SubjectMap"
    val subjectMap = modelWrapper.addResource(subjectMapStartString)
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RR.namespace + "subjectMap", subjectMap)

    //add termtype and class to subject map
    modelWrapper.addPropertyAsPropertyToResource(subjectMap, RdfNamespace.RR.namespace + "termType", RdfNamespace.RR.namespace + "BlankNode")
    modelWrapper.addPropertyAsPropertyToResource(subjectMap, RdfNamespace.RR.namespace + "class", RdfNamespace.FNO.namespace + "Execution")

    /**
      * add function pom
      */

    //add pom blank node to triples map
    val pomBlankNode = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RR.namespace + "predicateObjectMap", pomBlankNode)
    modelWrapper.addPropertyAsPropertyToResource(pomBlankNode, RdfNamespace.RR.namespace + "predicate", RdfNamespace.FNO.namespace + "executes")

    //add object map blank node to pom blank node
    val omBlankNode = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(pomBlankNode, RdfNamespace.RR.namespace + "objectMap", omBlankNode)
    modelWrapper.addPropertyAsPropertyToResource(omBlankNode, RdfNamespace.RR.namespace + "constant", RdfNamespace.DBF.namespace + "functionStartEndDate")

    /**
      * add start pom
      */

    //add pom blank node 2 to triples map
    val pomBlankNode2 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RR.namespace + "predicateObjectMap", pomBlankNode2)

    //add predicate to pom blank node 2
    modelWrapper.addPropertyAsPropertyToResource(pomBlankNode2, RdfNamespace.RR.namespace + "predicate", RdfNamespace.DBF.namespace + "startParameter")

    //add object map blank node to pom blank node 2
    val omBlankNode2 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(pomBlankNode2, RdfNamespace.RR.namespace + "objectMap", omBlankNode2)
    modelWrapper.addLiteralAsPropertyToResource(omBlankNode2, RdfNamespace.RR.namespace + "constant", mapping.startDateOntologyProperty.uri)

    /**
      * add end property pom
      */

    //add pom blank node 3 to triples map
    val pomBlankNode3 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RR.namespace + "predicateObjectMap", pomBlankNode3)

    //add predicate to pom blank node 3
    modelWrapper.addPropertyAsPropertyToResource(pomBlankNode3, RdfNamespace.RR.namespace + "predicate", RdfNamespace.DBF.namespace + "endParameter")

    //add object map blank node to pom blank node 3
    val omBlankNode3 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(pomBlankNode3, RdfNamespace.RR.namespace + "objectMap", omBlankNode3)
    modelWrapper.addLiteralAsPropertyToResource(omBlankNode3, RdfNamespace.RR.namespace + "constant", mapping.endDateOntologyProperty.uri)

    /**
      * add property pom
      */

    //add pom blank node 4 to triples map
    val pomBlankNode4 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(triplesMapStart, RdfNamespace.RR.namespace + "predicateObjectMap", pomBlankNode4)

    //add predicate to pom blank node 4
    modelWrapper.addPropertyAsPropertyToResource(pomBlankNode4, RdfNamespace.RR.namespace + "predicate", RdfNamespace.DBF.namespace + "propertyParameter")

    //add object map blank node to pom blank node 4
    val omBlankNode4 = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(pomBlankNode4, RdfNamespace.RR.namespace + "objectMap", omBlankNode4)
    modelWrapper.addLiteralAsPropertyToResource(omBlankNode4, RdfNamespace.RR.namespace + "constant", mapping.templateProperty)

  }

  /**
    * Returns the base name + name added
    */
  private def baseName(name : String): String =
  {
    "http://mappings.dbpedia.org/wiki/" + modelWrapper.wikiTitle.encodedWithNamespace + "/" + name
  }

  **/

}
