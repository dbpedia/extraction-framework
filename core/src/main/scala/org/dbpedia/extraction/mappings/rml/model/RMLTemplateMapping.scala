package org.dbpedia.extraction.mappings.rml.model

import org.dbpedia.extraction.mappings.rml.translate.format.RMLFormatter

/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(val title: String, modelWrapper: AbstractRMLModel) extends RMLMapping(modelWrapper: AbstractRMLModel) {


  def printAsNTriples: Unit = {
    modelWrapper.printAsNTriples
  }

  def printAsTurtle: Unit = {
    modelWrapper.printAsTurtle
  }

  def writeAsTurtle: String = {
    modelWrapper.writeAsTurtle
  }

  def writeAsTurtle(base: String) = {
    println(modelWrapper.writeAsTurtle(base))
    RMLFormatter.format(modelWrapper, base)
    //modelWrapper.writeAsTurtle(base : String)
  }

  def writeAsNTriples: String = {
    modelWrapper.writeAsNTriples
  }
}
