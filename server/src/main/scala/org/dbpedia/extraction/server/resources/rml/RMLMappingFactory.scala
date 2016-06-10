package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.{Model, ModelFactory, Property, Resource}
import org.dbpedia.extraction.mappings.{Extractor, TemplateMapping}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle}

/**
  * Abstract factory class for creating RML mappings
  * Contains logic for initiating an RML mapping with a triples map, logical source and subject map
  */
abstract class RMLMappingFactory {

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


  //setting prefixes in the model
  for(prefix <- prefixes) {
    model.setNsPrefix(prefix._1, prefix._2)
  }

  /**
    * Main method for creating the mappings
    */
  def createMapping(page: PageNode, language: Language, mapping : Extractor[Node]): RMLMapping

  protected def createNewTriplesMap(title: WikiTitle) = {
    model = ModelFactory.createDefaultModel() //every time this method is called a new instance of the model is made
    model.createResource(title.resourceIri)
          .addProperty(model.createProperty("a"), model.createProperty(prefixes.get("rr") + "triplesMap"))
          .addProperty(model.createProperty(prefixes.get("rml") + "logicalSource"), createLogicalSource(title))
          .addProperty(model.createProperty(prefixes.get("rml") + "subjectMap"), createSubjectMap(title))
  }

  protected def createRMLTemplateMapping = {
    new RMLTemplateMapping(model)
  }

  private def createLogicalSource(title: WikiTitle): Resource = {
    val ls = model.createResource(convertToLogicalSourceUri(title))
                  .addProperty(model.createProperty("a"), model.createProperty(prefixes.get("rml") + "logicalSource"))
                  .addProperty(model.createProperty(prefixes.get("rml") + "referenceFormulation"), model.createProperty(prefixes.get("ql") + "wikiText"))
                  .addProperty(model.createProperty(prefixes.get("rml") + "iterator"), "Infobox")
    return ls
  }

  private def createSubjectMap(title: WikiTitle): Resource = {
    val sm = model.createResource(convertToSubjectMapUri(title))
                  .addProperty(model.createProperty("a"), model.createProperty(prefixes.get("rml") + "subjectMap"))
    return sm
  }

  /**
    * Making sure that blank nodes have a proper uri
    */

  private def convertToLogicalSourceUri(title: WikiTitle): String = {
    title.resourceIri + "/Source/" + title.encoded.toString().trim
  }

  private def convertToSubjectMapUri(title: WikiTitle): String = {
    title.resourceIri + "/Class/" + title.encoded.toString().trim
  }



}
