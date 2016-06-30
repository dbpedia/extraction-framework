package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.ontology.RdfNamespace
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
  for(rdfNamespace <- RdfNamespace.prefixMap) {
    model.setNsPrefix(rdfNamespace._2.prefix, rdfNamespace._2.namespace)
  }


  def addMainLogicalSource(): Unit =
  {
    if(_logicalSource == null) {
      _logicalSource = addResourceWithPredicate(convertToLogicalSourceUri(wikiTitle), RdfNamespace.RML.namespace + "LogicalSource")
        .addProperty(createProperty(RdfNamespace.RML.namespace + "referenceFormulation"), createProperty(RdfNamespace.QL.namespace + "wikiText"))
        .addProperty(createProperty(RdfNamespace.RML.namespace + "iterator"), "Infobox")

    } else throw new IllegalStateException("Model already has a a logical source.")
  }


  def addMainSubjectMap(): Unit =
  {
    if(_subjectMap == null) {
      _subjectMap = addResourceWithPredicate(convertToSubjectMapUri(wikiTitle), RdfNamespace.RR.namespace + "SubjectMap")

    } else throw new IllegalStateException("Model already has a subject map.")
  }





  def addMainTriplesMap(): Unit =
  {
    if(_triplesMap == null) {
      _triplesMap = addResourceWithPredicate(wikiTitle.resourceIri, RdfNamespace.RR.namespace + "TriplesMap")
        .addProperty(createProperty(RdfNamespace.RML.namespace + "logicalSource"), logicalSource)
        .addProperty(createProperty(RdfNamespace.RR.namespace + "subjectMap"), subjectMap)

    } else throw new IllegalStateException("Model already has a triples map.")
  }


  //add a triples map with uri only
  def addTriplesMap(uri: String) : Resource =
  {
    addResourceWithPredicate(uri, RdfNamespace.RR.namespace + "TriplesMap")
  }

  //add a triples map with subject map
  def addTriplesMap(uri: String, subjectMap: Resource): Resource =
  {
    addResourceWithPredicate(uri, RdfNamespace.RR.namespace + "TriplesMap")
      .addProperty(createProperty(RdfNamespace.RML.namespace + "logicalSource"), logicalSource)
      .addProperty(createProperty(RdfNamespace.RR.namespace + "subjectMap"), subjectMap)
  }

  /**
    * Predicate object map methods
    */

  //add a predicate object map to model with uri only (no blank node)
  def addPredicateObjectMap(uri: String): Resource = {
    addResourceWithPredicate(uri, RdfNamespace.RR.namespace + "PredicateObjectMap")
  }

  //add a predicate object map with directly a given predicate and object
  def addPredicateObjectMapToResource(resource: Resource, predicate: String, _object: Resource) : Resource =
  {
    val predicateObjectMap = addBlankNode()
    addResourceAsPropertyToResource(resource, RdfNamespace.RR.namespace + "predicateObjectMap", predicateObjectMap)
    addPropertyAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "predicate", predicate)
    addResourceAsPropertyToResource(predicateObjectMap, RdfNamespace.RR.namespace + "objectMap", _object)
    predicateObjectMap
  }


  //add a predicate opject map via uri to a given triples map
  def addPredicateObjectMapUriToTriplesMap(predicateObjectMapUri: String, triplesMap: Resource) =
  {
    addPropertyAsPropertyToResource(triplesMap, RdfNamespace.RR.namespace + "predicateObjectMap", predicateObjectMapUri)

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
