package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings.GeoCoordinatesMapping
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.RMLModelWrapper

/**
  * Creates RML Mapping from GeoCoordinatesMapping and adds the triples to the given model
  */
class GeoCoordinatesRMLMapper(modelWrapper: RMLModelWrapper, mapping: GeoCoordinatesMapping) {


  def mapToModel() = {

    addGeoCoordinatesMapping()

  }

  def addGeoCoordinatesMapping() =
  {
    val uri = baseName("")
    addGeoCoordinatesMappingToTriplesMap(uri, modelWrapper.triplesMap)
  }

  def addGeoCoordinatesMappingToTriplesMap(uri: String, triplesMap: Resource) =
  {

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

    if(mapping.coordinates != null) {

      //add triples map to object map
      val parenTriplesMapUri = uniqueUri + "/ParentTriplesMap"
      val parentTriplesMap = modelWrapper.addTriplesMap(parenTriplesMapUri)
      modelWrapper.addResourceAsPropertyToResource(objectMap, RdfNamespace.RR.namespace + "parentTriplesMap", parentTriplesMap)

      //add logical source to paren triples map
      modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RML.namespace + "logicalSource", modelWrapper.logicalSource)

      //add subject map to parent triples map
      modelWrapper.addResourceAsPropertyToResource(parentTriplesMap, RdfNamespace.RR.namespace + "subjectMap", modelWrapper.subjectMap)

      val coordinatesUri = uniqueUri + "/coordinates"

      //first case: pair of coordinates is given
      addLatitudeOrLongitude(mapping, parentTriplesMap, coordinatesUri, "lat")
      addLatitudeOrLongitude(mapping, parentTriplesMap, coordinatesUri, "long")


    } else if (mapping.latitude != null && mapping.longitude != null) {

      modelWrapper.addLiteralAsPropertyToResource(objectMap, RdfNamespace.RML.namespace + "reference" ,"latitude")

    } else {

      val coordinatesUri = uniqueUri + "/degrees"
      //third case: degrees are given for calculating longitude and latitude
      //TODO: implement

    }
  }

  private def addLatitudeOrLongitude(mapping: GeoCoordinatesMapping, parentTriplesMap: Resource, coordinatesUri: String, latOrLong: String) =
  {

    //create predicate for latitude/longitude pom
    val latOrLongPredicate = RdfNamespace.GEO.namespace + latOrLong

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

    //create predicate for functionValue pom
    val functionValuePredicate = RdfNamespace.FNO.namespace + "executes"

    //create object map for functionValue pom
    val functionValueObjectMap = modelWrapper.addBlankNode()

    //add predicate and object map to pom
    val functionValuePom = modelWrapper.addPredicateObjectMapToResource(functionValueTriplesMap, functionValuePredicate, functionValueObjectMap)

    //add constant to object map
    modelWrapper.addPropertyAsPropertyToResource(functionValueObjectMap, RdfNamespace.RR.namespace + "constant", RdfNamespace.DBF.namespace + latOrLong)

    //create predicate for pom
    val functionValuePom2Predicate = RdfNamespace.DBF.namespace + latOrLong + "TemplateProperty"

    //create object map for pom
    val functionValueObjectMap2 = modelWrapper.addBlankNode()

    //add predicate and object map to pom
    modelWrapper.addPredicateObjectMapToResource(functionValuePom, functionValuePom2Predicate, functionValueObjectMap2)

    //add reference to object map
    modelWrapper.addLiteralAsPropertyToResource(functionValueObjectMap2, RdfNamespace.RML.namespace + "reference", mapping.coordinates)

  }

  /**
    * Returns the base name + name added
    */
  private def baseName(name : String): String =
  {
    "http://mappings.dbpedia.org/wiki/" + modelWrapper.wikiTitle.encodedWithNamespace + "/" + name
  }

}
