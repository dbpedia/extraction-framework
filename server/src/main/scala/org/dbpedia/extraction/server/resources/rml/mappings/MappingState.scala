package org.dbpedia.extraction.server.resources.rml.mappings

/**
  * Hold the state of the generation of a single mapping file, this is of course a stateful object
  */
class MappingState {

  private var _intermediateNodeMappingsDone = 0

  def incrementInterMediateNodeMappingsDone() = {
    _intermediateNodeMappingsDone += 1
  }

  def intermediateNodeMappingsDone = {
    _intermediateNodeMappingsDone
  }

}
