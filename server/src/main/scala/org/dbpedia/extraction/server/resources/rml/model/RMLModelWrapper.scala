package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * ModelWrapper with behaviour for RML
  */
class RMLModelWrapper(val wikiTitle: WikiTitle) extends ModelWrapper {

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


  def addMainLogicalSource(): Unit =
  {
    if(_logicalSource == null) {
      _logicalSource = addResourceWithPredicate(convertToLogicalSourceUri(wikiTitle), Prefixes("rml") + "LogicalSource")
        .addProperty(createProperty(Prefixes("rml") + "referenceFormulation"), createProperty(Prefixes("ql") + "wikiText"))
        .addProperty(createProperty(Prefixes("rml") + "iterator"), "Infobox")

    } else throw new IllegalStateException("Model already has a a logical source.")
  }


  def addMainSubjectMap(): Unit =
  {
    if(_subjectMap == null) {
      _subjectMap = addResourceWithPredicate(convertToSubjectMapUri(wikiTitle), Prefixes("rr") + "SubjectMap")

    } else throw new IllegalStateException("Model already has a subject map.")
  }





  def addMainTriplesMap(): Unit =
  {
    if(_triplesMap == null) {
      _triplesMap = addResourceWithPredicate(wikiTitle.resourceIri, Prefixes("rr") + "TriplesMap")
        .addProperty(createProperty(Prefixes("rml") + "logicalSource"), logicalSource)
        .addProperty(createProperty(Prefixes("rr") + "subjectMap"), subjectMap)

    } else throw new IllegalStateException("Model already has a triples map.")
  }


  //add a triples map with uri only
  def addTriplesMap(uri: String) : Resource =
  {
    addResourceWithPredicate(uri, Prefixes("rr") + "TriplesMap")
  }

  //add a triples map with subject map
  def addTriplesMap(uri: String, subjectMap: Resource): Resource =
  {
    addResourceWithPredicate(uri, Prefixes("rr") + "TriplesMap")
      .addProperty(createProperty(Prefixes("rml") + "logicalSource"), logicalSource)
      .addProperty(createProperty(Prefixes("rr") + "subjectMap"), subjectMap)
  }

  /**
    * Predicate object map methods
    */

  //add a predicate object map to model with uri only
  def addPredicateObjectMap(uri: String): Resource = {
    addResourceWithPredicate(uri, Prefixes("rr") + "PredicateObjectMap")
  }

  //add a predicate object map with directly a given predicate and object
  def addPredicateObjectMapToResource(resource: Resource, predicate: String, _object: Resource) =
  {
    val predicateObjectMap = addBlankNode()
    addResourceAsPropertyToResource(resource, Prefixes("rr") + "predicateObjectMap", predicateObjectMap)
    addPropertyAsPropertyToResource(predicateObjectMap, Prefixes("rr") + "predicate", predicate)
    addResourceAsPropertyToResource(predicateObjectMap, Prefixes("rr") + "objectMap", _object)
    predicateObjectMap
  }


  //add a predicate opject map to a given triples map
  def addPredicateObjectMapUriToTriplesMap(predicateObjectMapUri: String, triplesMap: Resource) =
  {
    addPropertyAsPropertyToResource(triplesMap, Prefixes("rr") + "predicateObjectMap", predicateObjectMapUri)

  }

  //add a predicate object map to the main triples map
  def addPredicateObjectMapToMainTriplesMap(predicate: String, _object: Resource): Resource =
  {
    addPredicateObjectMapToResource(triplesMap, predicate, _object)
  }

  /**
    *
    */

  //add resource to the model with predicate
  def addResourceWithPredicate(uri: String, predicate: String) : Resource = {
    model.createResource(uri, createProperty(predicate))
  }

  //add resource to the model with only a uri
  def addResource(uri: String): Resource =
  {
    model.createResource(uri)
  }



  /**
    * Utility functions
    */

  private def convertToLogicalSourceUri(title: WikiTitle): String =
  {
    title.resourceIri + "/Source/" + title.encoded.toString().trim
  }

  private def convertToSubjectMapUri(title: WikiTitle): String =
  {
    title.resourceIri + "/Class/" + title.encoded.toString().trim
  }

  private def createProperty(s: String) =
  {
    model.createProperty(s)
  }





}
