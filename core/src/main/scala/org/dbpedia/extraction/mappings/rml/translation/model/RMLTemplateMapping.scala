package org.dbpedia.extraction.mappings.rml.translation.model

import org.dbpedia.extraction.mappings.rml.translation.formatter.RMLFormatter

/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(val title: String, modelWrapper: RMLModel) extends RMLMapping {


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
    RMLFormatter.format(modelWrapper, base)
    //modelWrapper.writeAsTurtle(base : String)
  }

  def writeAsNTriples: String =
  {
    modelWrapper.writeAsNTriples
  }
}
