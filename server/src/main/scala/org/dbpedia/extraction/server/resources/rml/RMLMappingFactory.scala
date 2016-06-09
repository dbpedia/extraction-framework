package org.dbpedia.extraction.server.resources.rml

/**
  * Trait for creating RMLMappings
  */
trait RMLMappingFactory {
  def createMapping: RMLMapping
}
