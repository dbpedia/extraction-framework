package org.dbpedia.extraction.mappings.rml.translate.mapper

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.model.{AbstractRMLModel, RMLTranslationModel}
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}

/**
  * IntermediateNodeMapper
  */
class IntermediateNodeMapper(rmlModel: RMLTranslationModel, mapping: IntermediateNodeMapping, state : MappingState) {

  private val intermedatiaNodeMappingsNumber = state.intermediateNodeMappingsDone + 1
  state.incrementInterMediateNodeMappingsDone()

  def mapToModel() : List[RMLPredicateObjectMap] = {
    //addIntermediateNodeMapping() //TODO: enable these again
    List()
  }

  def addIndependentIntermediateNodeMapping() : List[RMLPredicateObjectMap] =
  {
    //create the predicate object map and add it to the triples map
    val uri = RMLUri(rmlModel.wikiTitle.resourceIri +
                            "/IntermediateNodeMapping/" +
                            mapping.nodeClass.name +
                            "/" +
                            mapping.correspondingProperty.name +
                            "__" +
                            intermedatiaNodeMappingsNumber)

    val intermediateNodePom = rmlModel.rmlFactory.createRMLPredicateObjectMap(uri)
    addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom)

    List(intermediateNodePom)
  }

  def addIntermediateNodeMapping() : List[RMLPredicateObjectMap] =
  {

    val uri = RMLUri(rmlModel.wikiTitle.resourceIri +
                          "/IntermediateNodeMapping/" +
                          mapping.nodeClass.name + "/" +
                          mapping.correspondingProperty.name +
                          "__" +
                          intermedatiaNodeMappingsNumber)

    val intermediateNodePom = rmlModel.triplesMap.addPredicateObjectMap(uri)

    addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom)

    List(intermediateNodePom)

  }

  private def addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom: RMLPredicateObjectMap) =
  {


    intermediateNodePom.addPredicate(RMLUri(mapping.correspondingProperty.uri))
    intermediateNodePom.addDCTermsType(new RMLLiteral("intermediateNodeMapping"))

    val intermediateNodeObjectMapUri = intermediateNodePom.uri.extend("/ObjectMap")
    val interMediateNodeObjectMap = intermediateNodePom.addObjectMap(intermediateNodeObjectMapUri)

    val parentTriplesMapUri = intermediateNodeObjectMapUri.extend("/ParentTriplesMap")
    val parentTriplesMap = interMediateNodeObjectMap.addParentTriplesMap(parentTriplesMapUri)
    parentTriplesMap.addLogicalSource(rmlModel.logicalSource)

    val parentSubjectMap = parentTriplesMap.addSubjectMap(parentTriplesMapUri.extend("/SubjectMap"))
    parentSubjectMap.addClass(RMLUri(mapping.nodeClass.uri))
    parentSubjectMap.addIRITermType()
    parentSubjectMap.addTemplate(new RMLLiteral("http://en.dbpedia.org/resource/{wikititle}__" + intermedatiaNodeMappingsNumber))
    if(mapping.correspondingProperty != null) parentSubjectMap.addRMLReference(new RMLLiteral(mapping.correspondingProperty.uri))

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