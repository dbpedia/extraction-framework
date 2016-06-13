package org.dbpedia.extraction.server.resources.rml

import org.dbpedia.extraction.server.resources.rml.model.ModelWrapper

/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(modelWrapper: ModelWrapper) extends RMLMapping {

  def printAsNTriples: Unit = {
    modelWrapper.printAsNTriples
  }

  def printAsTurtle: Unit = {
    modelWrapper.printAsTurtle
  }

  def writeAsTurtle: String = {
    modelWrapper.writeAsTurtle
  }


}
