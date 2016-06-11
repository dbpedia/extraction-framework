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
    * Setup of the factory
    */

  private var model: Model = ModelFactory.createDefaultModel()


  protected val prefixes = collection.immutable.HashMap(
    "rr" -> "http://www.w3.org/ns/r2rml#",
    "rml" -> "http://semweb.mmlab.be/ns/rml#",
    "ql" -> "http://semweb.mmlab.be/ns/ql#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "skos" -> "http://www.w3.org/2004/02/skos/core#",
    "dbo" -> "http://dbpedia.org/ontology/",
    "foaf" -> "http://xmlns.com/foaf/0.1/"
  )

  protected var page: PageNode = null
  protected var language: Language = null
  protected var triplesMap: Resource = null
  protected var subjectMap: Resource = null
  protected var logicalSource: Resource = null


  /**
    * Main method for creating the mappings
    */
  def createMapping(page: PageNode, language: Language, mappings: Mappings): RMLMapping


  /**
    * Common methods for instances of this factory
    */

  protected def createNewTriplesMap() = {

    //every time this method is called a new instance of the model is made
    model = ModelFactory.createDefaultModel()

    //setting predefined prefixes
    for(prefix <- prefixes) {
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

  protected def addPropertyToResource(subject: Resource, predicate: String, _object: String): Unit = {
    subject.addProperty(model.createProperty(predicate), model.createProperty(_object))
  }

  protected def addStringPropertyToResource(subject: Resource, predicate: String, _object: String): Unit = {
    subject.addProperty(model.createProperty(predicate), _object)
  }

  protected def addPropertyResource(subjectIRI: String, _object: String): Resource = {
    model.createResource(subjectIRI, model.createProperty(_object))
  }

  protected def addPropertyResource(subjectIRI: String): Resource = {
    model.createResource(subjectIRI)
  }

  protected def addResourcePropertyToResource(subject: Resource, predicate: String, _object: Resource): Unit = {
    subject.addProperty(model.createProperty(predicate), _object)
  }




  /**
    * Utility methods for creating new triples map
    */

  private def addLogicalSourceToModel(): Resource = {
    model.createResource(convertToLogicalSourceUri(page.title), model.createProperty(prefixes("rml") + "logicalSource"))
          .addProperty(model.createProperty(prefixes("rml") + "referenceFormulation"), model.createProperty(prefixes("ql") + "wikiText"))
          .addProperty(model.createProperty(prefixes("rml") + "iterator"), "Infobox")
  }

  private def addSubjectMapToModel(): Resource = {
    model.createResource(convertToSubjectMapUri(page.title), model.createProperty(prefixes("rr") + "subjectMap"))
  }

  private def addTriplesMapToModel(): Resource = {
    model.createResource(page.title.resourceIri, model.createProperty(prefixes("rr") + "triplesMap"))
          .addProperty(model.createProperty(prefixes("rml") + "logicalSource"), logicalSource)
          .addProperty(model.createProperty(prefixes("rr") + "subjectMap"), subjectMap)
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
