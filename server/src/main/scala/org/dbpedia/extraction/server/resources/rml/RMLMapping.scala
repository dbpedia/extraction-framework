package org.dbpedia.extraction.server.resources.rml

/**
  * RMLMappings converted from DBpedia mappings
  */
abstract class RMLMapping {

  def printAsNTriples: Unit

  def printAsTurtle: Unit

  def writeAsTurtle: String
}
