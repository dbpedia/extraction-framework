package org.dbpedia.extraction.server.resources.rml

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource}
import org.dbpedia.extraction.mappings.{Extractor, Mappings, TemplateMapping}
import org.dbpedia.extraction.server.resources.rml.model.{ModelWrapper, Prefixes, RMLModelWrapper}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle}

import collection.JavaConverters._
/**
  * Abstract factory class for creating RML mappings
  * Contains logic for initiating an RML mapping with a triples map, logical source and subject map
  */
abstract class RMLMappingFactory {

  protected var page: PageNode = null
  protected var language: Language = null
  protected var modelWrapper: RMLModelWrapper = null

  /**
    * Main method for creating the mappings
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLMapping


  /**
    * Common methods for instances of this factory
    */

  protected def createNewModelWithTriplesMap() =
  {

    //every time this method is called a new instance of the model is made and put into a wrapper
    modelWrapper = new RMLModelWrapper()

    // triples map, logical source and subject map resources created and added to the model
    modelWrapper.addLogicalSourceToModel(page.title)
    modelWrapper.addSubjectMapToModel(page.title)
    modelWrapper.addTriplesMapToModel(page.title)

  }

  protected def createRMLTemplateMapping =
  {
    new RMLTemplateMapping(modelWrapper)
  }







}
