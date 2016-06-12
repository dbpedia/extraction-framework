package org.dbpedia.extraction.server.resources.rml

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource}
import org.dbpedia.extraction.mappings.{Extractor, Mappings, TemplateMapping}
import org.dbpedia.extraction.server.resources.rml.util.{ModelWrapper, Prefixes}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle}

import collection.JavaConverters._
/**
  * Abstract factory class for creating RML mappings
  * Contains logic for initiating an RML mapping with a triples map, logical source and subject map
  */
abstract class RMLMappingFactory {

  /**
    * Setup of the factory
    */

  private var model: Model = ModelFactory.createDefaultModel()

  protected var page: PageNode = null
  protected var language: Language = null
  protected var triplesMap: Resource = null
  protected var subjectMap: Resource = null
  protected var logicalSource: Resource = null
  protected var modelWrapper: ModelWrapper = null


  /**
    * Main method for creating the mappings
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLMapping


  /**
    * Common methods for instances of this factory
    */

  protected def createNewTriplesMap() = {

    //every time this method is called a new instance of the model is made and put into a wrapper
    model = ModelFactory.createDefaultModel()
    modelWrapper = new ModelWrapper(model)

    //setting predefined prefixes
    for(prefix <- Prefixes.map) {
      model.setNsPrefix(prefix._1, prefix._2)
    }

    // triples map, logical source and subject map resources created and added to the model
    logicalSource = addLogicalSourceToModel()
    subjectMap = addSubjectMapToModel()
    triplesMap = addTriplesMapToModel()

  }

  protected def createRMLTemplateMapping = {
    new RMLTemplateMapping(model)
  }

  /**
    * Utility methods for creating new triples map
    */

  private def addLogicalSourceToModel(): Resource = {
    model.createResource(convertToLogicalSourceUri(page.title), model.createProperty(Prefixes("rml") + "logicalSource"))
          .addProperty(model.createProperty(Prefixes("rml") + "referenceFormulation"), model.createProperty(Prefixes("ql") + "wikiText"))
          .addProperty(model.createProperty(Prefixes("rml") + "iterator"), "Infobox")
  }

  private def addSubjectMapToModel(): Resource = {
    model.createResource(convertToSubjectMapUri(page.title), model.createProperty(Prefixes("rr") + "subjectMap"))
  }

  private def addTriplesMapToModel(): Resource = {
    model.createResource(page.title.resourceIri, model.createProperty(Prefixes("rr") + "triplesMap"))
          .addProperty(model.createProperty(Prefixes("rml") + "logicalSource"), logicalSource)
          .addProperty(model.createProperty(Prefixes("rr") + "subjectMap"), subjectMap)
  }

  /**
    * Methods to give a blank nodes a proper uri
    */

  private def convertToLogicalSourceUri(title: WikiTitle): String = {
    title.resourceIri + "/Source/" + title.encoded.toString().trim
  }

  private def convertToSubjectMapUri(title: WikiTitle): String = {
    title.resourceIri + "/Class/" + title.encoded.toString().trim
  }



}
