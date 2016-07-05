package org.dbpedia.extraction.server.resources.rml.mappings

import be.ugent.mmlab.rml.model.TriplesMap
import org.dbpedia.extraction.mappings.ConstantMapping
import org.dbpedia.extraction.server.resources.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.model.rmlresources.RMLTriplesMap

/**
  * Creates RML Mapping from Constant Mappings and adds the triples to the given model
  */
class ConstantRMLMapper(rmlModel: RMLModel, mapping: ConstantMapping) {

  private val rmlFactory = rmlModel.rmlFactory

  def mapToModel() = {
    addConstantMapping()
  }

  def addConstantMapping() =
  {
    val uniqueUri = rmlModel.wikiTitle.resourceIri
    addConstantMappingToTriplesMap(uniqueUri, rmlModel.triplesMap)
  }

  def addConstantMappingToTriplesMap(uri: String, triplesMap: RMLTriplesMap) =
  {
    //TODO
  }

}
