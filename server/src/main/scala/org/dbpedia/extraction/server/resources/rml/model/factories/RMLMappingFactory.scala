package org.dbpedia.extraction.server.resources.rml.model.factories

import org.dbpedia.extraction.mappings.Mappings
import org.dbpedia.extraction.server.resources.rml.model.RMLMapping
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode
/**
  * Abstract factory class for creating RML mappings
  * Contains logic for initiating an RML mapping with a triples map, logical source and subject map
  */
abstract class RMLMappingFactory {

  /**
    * Main method for creating the mappings
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLMapping

}
