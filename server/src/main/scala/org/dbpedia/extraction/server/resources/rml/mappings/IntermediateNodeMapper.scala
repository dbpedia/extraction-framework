package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

/**
  * IntermediateNodeMapper
  */
class IntermediateNodeMapper(rmlModel: RMLModel, mapping: IntermediateNodeMapping) {


  def mapToModel() = {
    addIntermediateNodeMapping()
  }

  def addIntermediateNodeMapping() =
  {

    //create the predicate object map and it to the triples map
    val uri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/IntermediateNodeMapping/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name)
    val intermediateNodePom = rmlModel.triplesMap.addPredicateObjectMap(uri)
    intermediateNodePom.addPredicate(new RMLUri(mapping.correspondingProperty.uri))
    intermediateNodePom.addDCTermsType(new RMLLiteral("intermediateNodeMapping"))

    val intermediateNodeObjectMapUri = uri.extend("/ObjectMap")
    val interMediateNodeObjectMap = intermediateNodePom.addObjectMap(intermediateNodeObjectMapUri)

    val parentTriplesMapUri = intermediateNodeObjectMapUri.extend("/ParentTriplesMap")
    val parentTriplesMap = interMediateNodeObjectMap.addParentTriplesMap(parentTriplesMapUri)
    parentTriplesMap.addLogicalSource(rmlModel.logicalSource)

    val parentSubjectMap = parentTriplesMap.addSubjectMap(parentTriplesMapUri.extend("/SubjectMap"))
    parentSubjectMap.addClass(new RMLUri(mapping.nodeClass.uri))
    parentSubjectMap.addTermTypeIRI()
    parentSubjectMap.addConstant(new RMLLiteral(rmlModel.wikiTitle.resourceIri + "/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name))

    //create the intermediate mappings
    for(mapping <- mapping.mappings) {
      addPropertyMapping(mapping, parentTriplesMap)
    }

  }

  /**
    * Adds mappings (this is used by intermediate node mappings
    */
  private def addPropertyMapping(mapping: PropertyMapping, triplesMap: RMLTriplesMap) =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    mapping.getClass.getSimpleName match {
      case "SimplePropertyMapping" => rmlMapper.addSimplePropertyMappingToTriplesMap(mapping.asInstanceOf[SimplePropertyMapping], triplesMap)
      case "CalculateMapping" => println("Intermediate Calculate Mapping not supported.")
      case "CombineDateMapping" => println("Intermediate Combine Date Mapping not supported.")
      case "DateIntervalMapping" => rmlMapper.addDateIntervalMappingToTriplesMap(mapping.asInstanceOf[DateIntervalMapping], triplesMap)
      case "GeoCoordinatesMapping" => rmlMapper.addGeoCoordinatesMappingToTriplesMap(mapping.asInstanceOf[GeoCoordinatesMapping], triplesMap)
      case "ConstantMapping" => rmlMapper.addConstantMappingToTriplesMap(mapping.asInstanceOf[ConstantMapping], triplesMap)
    }
  }

}
