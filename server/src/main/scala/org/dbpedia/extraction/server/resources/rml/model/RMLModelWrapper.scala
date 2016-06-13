package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * ModelWrapper with behaviour for RML
  */
class RMLModelWrapper extends ModelWrapper {

  private var _logicalSource: Resource = null
  private var _subjectMap: Resource = null
  private var _triplesMap: Resource = null

  def logicalSource = _logicalSource
  def subjectMap = _subjectMap
  def triplesMap = _triplesMap

  //setting predefined prefixes
  for(prefix <- Prefixes.map) {
    model.setNsPrefix(prefix._1, prefix._2)
  }

  /**
    * Methods for creating new triples map
    */

  def addLogicalSourceToModel(wikiTitle: WikiTitle): Unit = {
    if(_logicalSource == null) {
      _logicalSource = createResource(convertToLogicalSourceUri(wikiTitle), createProperty(Prefixes("rml") + "LogicalSource"))
        .addProperty(createProperty(Prefixes("rml") + "referenceFormulation"), createProperty(Prefixes("ql") + "wikiText"))
        .addProperty(createProperty(Prefixes("rml") + "iterator"), "Infobox")

    } else throw new IllegalStateException("Model already has a a logical source.")
  }

  def addSubjectMapToModel(wikiTitle: WikiTitle): Unit = {
    if(_subjectMap == null) {
      _subjectMap = createResource(convertToSubjectMapUri(wikiTitle), createProperty(Prefixes("rr") + "SubjectMap"))

    } else throw new IllegalStateException("Model already has a subject map.")
  }

  def addTriplesMapToModel(wikiTitle: WikiTitle): Unit = {
    if(_triplesMap == null) {
      _triplesMap = createResource(wikiTitle.resourceIri, createProperty(Prefixes("rr") + "TriplesMap"))
        .addProperty(createProperty(Prefixes("rml") + "logicalSource"), logicalSource)
        .addProperty(createProperty(Prefixes("rr") + "subjectMap"), subjectMap)

    } else throw new IllegalStateException("Model already has a triples map.")
  }

  private def convertToLogicalSourceUri(title: WikiTitle): String = {
    title.resourceIri + "/Source/" + title.encoded.toString().trim
  }

  private def convertToSubjectMapUri(title: WikiTitle): String = {
    title.resourceIri + "/Class/" + title.encoded.toString().trim
  }



  def addPredicateObjectMapToResource(resource: Resource, predicate: String, _object: Resource) = {
    val predicateObjectMap = addBlankNode()
    addResourceAsPropertyToResource(resource, Prefixes("rr") + "predicateObjectMap", predicateObjectMap)
    addPropertyAsPropertyToResource(predicateObjectMap, Prefixes("rr") + "predicate", predicate)
    addResourceAsPropertyToResource(predicateObjectMap, Prefixes("rr") + "objectMap", _object)
    predicateObjectMap
  }

  def addPredicateObjectMapToRoot(predicate: String, _object: Resource): Resource = {
    addPredicateObjectMapToResource(triplesMap, predicate, _object)
  }



  private def createResource(s: String, p: Property) = {
    model.createResource(s, p)
  }

  private def createProperty(s: String) = {
    model.createProperty(s)
  }



}
