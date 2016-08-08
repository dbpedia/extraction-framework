package org.dbpedia.extraction.server.resources.rml.mappings

import org.apache.jena.rdf.model.Resource
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.server.resources.rml.model.RMLModel

/**
  * IntermediateNodeMapper
  */
class IntermediateNodeMapper(rmlModel: RMLModel, mapping: IntermediateNodeMapping) {


  def mapToModel() : List[RMLPredicateObjectMap] = {
    addIntermediateNodeMapping()
  }

  def addIndependentIntermediateNodeMapping() : List[RMLPredicateObjectMap] =
  {
    //create the predicate object map and add it to the triples map
    val uri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/IntermediateNodeMapping/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name)
    val intermediateNodePom = rmlModel.rmlFactory.createRMLPredicateObjectMap(uri)
    addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom)

    List(intermediateNodePom)
  }

  def addIntermediateNodeMapping() : List[RMLPredicateObjectMap] =
  {

    val uri = new RMLUri(rmlModel.wikiTitle.resourceIri + "/IntermediateNodeMapping/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name)
    val intermediateNodePom = rmlModel.triplesMap.addPredicateObjectMap(uri)

    addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom)

    List(intermediateNodePom)

  }

  private def addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom: RMLPredicateObjectMap) =
  {
    intermediateNodePom.addPredicate(new RMLUri(mapping.correspondingProperty.uri))
    intermediateNodePom.addDCTermsType(new RMLLiteral("intermediateNodeMapping"))

    val intermediateNodeObjectMapUri = intermediateNodePom.uri.extend("/ObjectMap")
    val interMediateNodeObjectMap = intermediateNodePom.addObjectMap(intermediateNodeObjectMapUri)

    val parentTriplesMapUri = intermediateNodeObjectMapUri.extend("/ParentTriplesMap")
    val parentTriplesMap = interMediateNodeObjectMap.addParentTriplesMap(parentTriplesMapUri)
    parentTriplesMap.addLogicalSource(rmlModel.logicalSource)

    val parentSubjectMap = parentTriplesMap.addSubjectMap(parentTriplesMapUri.extend("/SubjectMap"))
    parentSubjectMap.addClass(new RMLUri(mapping.nodeClass.uri))
    parentSubjectMap.addTermTypeIRI()
    parentSubjectMap.addConstant(new RMLLiteral("http://en.dbpedia.org/resource/{{wikititle}}/" + mapping.nodeClass.name + "/" + mapping.correspondingProperty.name))

    //create the intermediate mappings
    for(mapping <- mapping.mappings) {
      addPropertyMapping(mapping, parentTriplesMap)
    }
  }

  private def addPropertyMapping(mapping: PropertyMapping, triplesMap: RMLTriplesMap) =
  {
    val rmlMapper = new RMLModelMapper(rmlModel)
    rmlMapper.addMappingToTriplesMap(mapping, triplesMap)
  }

}
