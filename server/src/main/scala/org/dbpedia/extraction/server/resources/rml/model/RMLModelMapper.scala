package org.dbpedia.extraction.server.resources.rml.model

import java.util.Date

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.{ConditionalMapping, GeoCoordinatesMapping, IntermediateNodeMapping, _}
import org.dbpedia.extraction.ontology.{OntologyProperty, RdfNamespace}
import org.dbpedia.extraction.ontology.datatypes.Datatype

/**
  * Class that adds rml mappings to a ModelWrapper
  */
class RMLModelMapper(modelWrapper: RMLModelWrapper) {

  //TODO: this needs to be refactored into seperate classes

  def addSimplePropertyMapping(mapping: SimplePropertyMapping) =
  {
    val uniqueUri = baseName("")
    addSimplePropertyMappingToTriplesMap(mapping, modelWrapper.triplesMap, uniqueUri)
  }

  private def addSimplePropertyMappingToTriplesMap(mapping: SimplePropertyMapping, triplesMap: Resource, uri: String) =
  {

    //create predicate object map
    val predicateObjectMap = modelWrapper.addPredicateObjectMap(uri + "SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty)

    //add dcterms type to predicate map
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.DCTERMS.namespace + "type", RdfNamespace.DBF.namespace + "simplePropertyMapping" )

    //add predicate to predicate object map
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "predicate", mapping.ontologyProperty.uri)

    //add object map with rml reference
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
    modelWrapper.addLiteralAsPropertyToResource(objectMap, RdfNamespace.RML.namespace + "reference", mapping.templateProperty)

    //add unit if present
    if(mapping.unit != null) addUnitToPredicateObjectMap(predicateObjectMap, mapping.unit)

    //add predicate object map to triples map
    modelWrapper.addPredicateObjectMapUriToTriplesMap(uri + "SimplePropertyMapping/" + mapping.ontologyProperty.name + "/" + mapping.templateProperty, triplesMap)

  }




  def addCalculateMapping(mapping: CalculateMapping) =
  {
    //TODO: implement
    println("Calculate Mappings not supported")
  }





  def addCombineDateMapping(mapping: CombineDateMapping) =
  {
    //TODO: implement
    println("Combine Date Mappings not supported")
  }





  def addDateIntervalMapping(mapping: DateIntervalMapping) =
  {
    val uniqueUri= baseName("")
    addDateIntervalMappingToTriplesMap(mapping, modelWrapper.triplesMap, uniqueUri)
  }

  private def addDateIntervalMappingToTriplesMap(mapping: DateIntervalMapping, triplesMap : Resource, uri: String) =
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



  def addGeoCoordinatesMapping(mapping: GeoCoordinatesMapping) =
  {
    val uri = baseName("")
    addGeoCoordinatesMappingToTriplesMap(mapping, uri, modelWrapper.triplesMap)
  }

  def addGeoCoordinatesMappingToTriplesMap(mapping: GeoCoordinatesMapping, uri: String, triplesMap: Resource) =
  {

    println("Geocoordinates Mapping not fully supported: " + mapping.ontologyProperty.name)

    val uniqueUri = uri + "GeoCoordinatesMapping/" + mapping.ontologyProperty.name

    //add pom to triples map
    val geoCoordinatesPom = modelWrapper.addPredicateObjectMap(uniqueUri)
    modelWrapper.addResourceAsPropertyToResource(triplesMap, RdfNamespace.RR.namespace + "predicateObjectMap", geoCoordinatesPom)

    //add dcterms:type to predicate
    modelWrapper.addPropertyAsPropertyToResource(geoCoordinatesPom, RdfNamespace.DCTERMS.namespace + "type", RdfNamespace.DCTERMS.namespace + "geoCoordinatesMapping")

    //add predicate to pom
    modelWrapper.addPropertyAsPropertyToResource(geoCoordinatesPom, RdfNamespace.RR.namespace + "predicate", mapping.ontologyProperty.uri)

    //add object map to pom
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(geoCoordinatesPom, RdfNamespace.RR.namespace + "objectMap", objectMap)

    //add triples map to object map
    val parenTriplesMapUri = uniqueUri + "/ParentTriplesMap"
    val parentTriplesMap = modelWrapper.addTriplesMap(parenTriplesMapUri)
    modelWrapper.addResourceAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", parentTriplesMap)

    //add logical source to paren triples map
    modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RML.namespace + "logicalSource", modelWrapper.logicalSource)

    //add subject map to parent triples map
    modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RR.namespace + "subjectMap", modelWrapper.subjectMap)


    if(mapping.coordinates != null) {

      println(mapping.ontologyProperty + " : " + mapping.coordinates)


      val coordinatesUri = uniqueUri + "/coordinates"
      //first case: pair of coordinates is given
      //TODO: implement

    } else if (mapping.latitude != null && mapping.longitude != null) {

      val coordinatesUri = uniqueUri + "/latitude_longitude"

      println("Mapping with latitude and longitude present")

      //second case: only latitude and longitude is given

      addLatitudeOrLongitude(mapping, parentTriplesMap, coordinatesUri, "lat")
      addLatitudeOrLongitude(mapping, parentTriplesMap, coordinatesUri, "long")


    } else {

      val coordinatesUri = uniqueUri + "/degrees"
      //third case: degrees are given for calculating longitude and latitude
      //TODO: implement

    }
  }


  private def addLatitudeOrLongitude(mapping: GeoCoordinatesMapping, parentTriplesMap: Resource, coordinatesUri: String, latOrLong: String) =
  {

    //create predicate for latitude/longitude pom
    val latOrLongPredicate = RdfNamespace.RR + "predicate"

    //create object map for latitude/longitude pom
    val latitudeObjectMapUri = coordinatesUri + "/" + latOrLong + "ObjectMap"
    val latitudeObjectMap = modelWrapper.addResourceWithPredicate(latitudeObjectMapUri, RdfNamespace.FNML.namespace + "FunctionTermMap")

    //add predicate and object map to latitude pom
    modelWrapper.addPredicateObjectMapToResource(parentTriplesMap, latOrLongPredicate, latitudeObjectMap)

    //add function value triples map to object map
    val functionValueTriplesMapUri = latitudeObjectMapUri + "/FunctionValueTriplesMap"
    val functionValueTriplesMap = modelWrapper.addTriplesMap(functionValueTriplesMapUri)
    modelWrapper.addResourceAsPropertyToResource(latitudeObjectMap, RdfNamespace.FNML.namespace + "functionValue", functionValueTriplesMap)

    //add logical source to triples map
    modelWrapper.addResourceAsPropertyToResource(functionValueTriplesMap, RdfNamespace.RML.namespace + "logicalSource", modelWrapper.logicalSource)

    //add subject map to triples map
    val functionSubjectMap = modelWrapper.addResource(coordinatesUri + "/FunctionSubjectMap")
    modelWrapper.addResourceAsPropertyToResource(functionValueTriplesMap, RdfNamespace.RR.namespace + "subjectMap", functionSubjectMap)

    //add termType and class to subject map
    modelWrapper.addPropertyAsPropertyToResource(functionSubjectMap, RdfNamespace.RR.namespace + "termType", RdfNamespace.RR.namespace + "BlankNode")
    modelWrapper.addPropertyAsPropertyToResource(functionSubjectMap, RdfNamespace.RR.namespace + "class", RdfNamespace.FNO.namespace + "Execution")

    //create pom to functionValueTriplesMap
    val functionValuePom = modelWrapper.addBlankNode()

    //create predicate for functionValue pom
    val functionValuePredicate = RdfNamespace.FNO.namespace + "executes"

    //create object map for functionValue pom
    val functionValueObjectMap = modelWrapper.addBlankNode()

    //add predicate and object map to pom
    modelWrapper.addPredicateObjectMapToResource(functionValuePom, functionValuePredicate, functionValueObjectMap)

    //add constant to object map
    modelWrapper.addPropertyAsPropertyToResource(functionValueObjectMap, RdfNamespace.RR.namespace + "constant", RdfNamespace.DBF.namespace + latOrLong)

    //create pom for functionValuePom
    val functionValuePom2 = modelWrapper.addBlankNode()

    //create predicate for pom
    val functionValuePom2Predicate = RdfNamespace.DBF.namespace + latOrLong + "TemplateProperty"

    //create object map for pom
    val functionValueObjectMap2 = modelWrapper.addBlankNode()

    //add predicate and object map to pom
    modelWrapper.addPredicateObjectMapToResource(functionValuePom2, functionValuePom2Predicate, functionValueObjectMap2)

    //add reference to object map
    modelWrapper.addPropertyAsPropertyToResource(functionValueObjectMap2, RdfNamespace.RML.namespace, mapping.ontologyProperty.uri)

  }


  def addConditionalMapping(mapping: ConditionalMapping) =
  {
    //TODO: implement
    println("Conditional Mappings not supported")
  }





  def addIntermediateNodeMapping(mapping: IntermediateNodeMapping) =
  {

    //create the predicate object map and it to the triples map
    val templateString = "IntermediateNodeMapping/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name
    val uniqueString = baseName(templateString)
    val predicateObjectMap = modelWrapper.addPredicateObjectMap(uniqueString)
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "predicate", mapping.correspondingProperty.uri)
    modelWrapper.addPredicateObjectMapUriToTriplesMap(uniqueString, modelWrapper.triplesMap)

    //add dcterms:type to predicate:
    modelWrapper.addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.DCTERMS.namespace + "type", RdfNamespace.DBF.namespace + "intermediateNodeMapping" )

    //create the triples map with its subject map and object map
    val subjectMap = modelWrapper.addResourceWithPredicate(baseName(templateString + "/SubjectMap"), RdfNamespace.RR.namespace + "SubjectMap")
    modelWrapper.addPropertyAsPropertyToResource(subjectMap, RdfNamespace.RR.namespace + "constant", "tobeDefined")
    val triplesMap = modelWrapper.addTriplesMap(baseName(templateString + "/TriplesMap"), subjectMap)
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addResourceAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", triplesMap)
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)

    //create the intermediate mappings
    for(mapping <- mapping.mappings) {
      addPropertyMapping(mapping, triplesMap)
    }

  }

  def addConstantMapping(mapping: ConstantMapping) =
  {
    //TODO: implement
    println("Constant Mappings not supported")
  }




  /**
    * Returns the base name + name added
    */
  private def baseName(name : String): String =
  {
    "http://mappings.dbpedia.org/wiki/" + modelWrapper.wikiTitle.encodedWithNamespace + "/" + name
  }

  private def addUnitToPredicateObjectMap(predicateObjectMap: Resource, unit : Datatype): Unit =
  {
    val objectMap = modelWrapper.addBlankNode()
    modelWrapper.addPropertyAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", unit.uri)
    modelWrapper.addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", objectMap)
  }

  /**
    * Adds mappings (this is used by intermediate node mappings
    */
  private def addPropertyMapping(mapping: PropertyMapping, triplesMap: Resource) =
  {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => addSimplePropertyMappingToTriplesMap(mapping.asInstanceOf[SimplePropertyMapping],
        triplesMap, triplesMap.getNameSpace)
      case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
      case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
      case "DateIntervalMapping" => addDateIntervalMappingToTriplesMap(mapping.asInstanceOf[DateIntervalMapping], triplesMap, triplesMap.getNameSpace)
      case "GeoCoordinatesMapping" => println("Intermediate GeoCoordinates Mapping not supported.")
      case "ConditionalMapping" => println("Intermediate Conditional Mapping not supported.")
      case "IntermediateNodeMapping" => println("Intermediate Intermediate Mapping not supported.")
      case "ConstantMapping" => println("Constant Mapping not supported.")
    }
  }
  
}
