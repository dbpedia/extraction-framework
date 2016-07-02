package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.{RMLModel, RMLResourceFactory}

/**
  * Created by wmaroy on 30.06.16.
  */
class IntermediateNodeMapper(rmlFactory: RMLResourceFactory, modelWrapper: RMLModel, mapping: IntermediateNodeMapping) {

  /**

  def mapToModel() = {
    addIntermediateNodeMapping()
  }

  def addIntermediateNodeMapping() =
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

  /**
    * Adds mappings (this is used by intermediate node mappings
    */
  private def addPropertyMapping(mapping: PropertyMapping, triplesMap: Resource) =
  {
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => new SimplePropertyRMLMapper(rmlFactory, mapping.asInstanceOf[SimplePropertyMapping]).addSimplePropertyMappingToTriplesMap(triplesMap.getNameSpace, triplesMap)
      case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
      case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
      case "DateIntervalMapping" => new DateIntervalRMLMapper(modelWrapper, mapping.asInstanceOf[DateIntervalMapping]).addDateIntervalMappingToTriplesMap(triplesMap.getNameSpace, triplesMap)
      case "GeoCoordinatesMapping" => new GeoCoordinatesRMLMapper(modelWrapper, mapping.asInstanceOf[GeoCoordinatesMapping]).addGeoCoordinatesMappingToTriplesMap(triplesMap.getNameSpace, triplesMap)
      case "ConditionalMapping" => println("Intermediate Conditional Mapping not supported.")
      case "IntermediateNodeMapping" => println("Intermediate Intermediate Mapping not supported.")
      case "ConstantMapping" => println("Constant Mapping not supported.")
    }
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
