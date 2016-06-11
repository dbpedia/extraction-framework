package org.dbpedia.extraction.server.resources.rml

import be.ugent.mmlab.rml.model.RMLMapping
import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource}
import org.dbpedia.extraction.mappings.{Extractor, Mappings, TemplateMapping}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle}
import collection.JavaConverters._
/**
  * Abstract factory class for creating RML mappings
  * Contains logic for initiating an RML mapping with a triples map, logical source and subject map
  */
abstract class RMLMappingFactory {

  /**
    * Instantiating the factory
    */

  private var model: Model = ModelFactory.createDefaultModel()
  private val prefixes = collection.immutable.HashMap(
    "rr" -> "http://www.w3.org/ns/r2rml#",
    "rml" -> "http://semweb.mmlab.be/ns/rml#",
    "ql" -> "http://semweb.mmlab.be/ns/ql#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "skos" -> "http://www.w3.org/2004/02/skos/core#",
    "dbo" -> "http://dbpedia.org/ontology/",
    "foaf" -> "http://xmlns.com/foaf/0.1/"
  )





  /**
    * Main method for creating the mappings
    */

  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLMapping

  /**
    * Common methods for instances of this factory
    */

  protected def createNewTriplesMap(title: WikiTitle) = {
    model = ModelFactory.createDefaultModel() //every time this method is called a new instance of the model is made
    for(prefix <- prefixes) {
      model.setNsPrefix(prefix._1, prefix._2)
    }
    model.createResource(title.resourceIri, model.createProperty(prefixes.get("rr").get + "triplesMap"))
          .addProperty(model.createProperty(prefixes.get("rml").get + "logicalSource"), createLogicalSource(title))
          .addProperty(model.createProperty(prefixes.get("rr").get + "subjectMap"), createSubjectMap(title))
  }

  protected def createRMLTemplateMapping = {
    new RMLTemplateMapping(model)
  }

  /**
    * Utility methods for creating new triples map
    */

  private def createLogicalSource(title: WikiTitle): Resource = {
    val ls = model.createResource(convertToLogicalSourceUri(title), model.createProperty(prefixes.get("rml").get + "logicalSource"))
                  .addProperty(model.createProperty(prefixes.get("rml").get + "referenceFormulation"), model.createProperty(prefixes.get("ql").get + "wikiText"))
                  .addProperty(model.createProperty(prefixes.get("rml").get + "iterator"), "Infobox")
    ls
  }

  private def createSubjectMap(title: WikiTitle): Resource = {
    val sm = model.createResource(convertToSubjectMapUri(title), model.createProperty(prefixes.get("rr").get + "subjectMap"))
    sm
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
