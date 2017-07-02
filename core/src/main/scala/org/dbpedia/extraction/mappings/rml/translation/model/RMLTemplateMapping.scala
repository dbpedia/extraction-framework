package org.dbpedia.extraction.mappings.rml.translation.model

/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(val title: String, modelWrapper: ModelWrapper) extends RMLMapping {


  def printAsNTriples: Unit =
  {
    modelWrapper.printAsNTriples
  }

  def printAsTurtle: Unit =
  {
    modelWrapper.printAsTurtle
  }

  def writeAsTurtle: String =
  {
    modelWrapper.writeAsTurtle
  }

  def writeAsTurtle(base : String) =
  {
    modelWrapper.writeAsTurtle(base : String)
  }

  def writeAsNTriples: String =
  {
    modelWrapper.writeAsNTriples
  }
}
