package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource}
import org.dbpedia.extraction.mappings.{Extractor, Mappings, TemplateMapping}
import org.dbpedia.extraction.server.resources.rml.model.{ModelWrapper, RMLModel, RMLResourceFactory}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle}

import collection.JavaConverters._
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
