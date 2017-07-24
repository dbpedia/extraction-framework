package org.dbpedia.extraction.mappings.rml.translate.mapper

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
