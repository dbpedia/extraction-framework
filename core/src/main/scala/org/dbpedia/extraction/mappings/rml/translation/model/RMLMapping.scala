package org.dbpedia.extraction.mappings.rml.translation.model

/**
  * RMLMappings converted from DBpedia mappings
  */
abstract class RMLMapping {

  def printAsNTriples: Unit

  def printAsTurtle: Unit

  def writeAsTurtle: String

  def writeAsNTriples: String
}
