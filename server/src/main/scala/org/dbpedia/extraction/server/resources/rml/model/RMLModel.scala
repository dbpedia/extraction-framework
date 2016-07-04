package org.dbpedia.extraction.server.resources.rml.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.server.resources.rml.model.rmlresources._
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * ModelWrapper with behaviour for RML
  */
class RMLModel(val wikiTitle: WikiTitle, val sourceUri : String) extends ModelWrapper {

  val rmlFactory = new RMLResourceFactory(model)

  private val _triplesMap: RMLTriplesMap = rmlFactory.createRMLTriplesMap(new RMLUri(wikiTitle.resourceIri))
  private val _subjectMap: RMLSubjectMap = _triplesMap.addSubjectMap(new RMLUri(convertToSubjectMapUri(wikiTitle)))
  private val _logicalSource: RMLLogicalSource = _triplesMap.addLogicalSource(new RMLUri(convertToLogicalSourceUri(wikiTitle)))
  private val _functionSubjectMap: RMLSubjectMap = rmlFactory.createRMLSubjectMap(new RMLUri(convertToSubjectMapUri(wikiTitle) + "/Function"))

  def logicalSource = _logicalSource
  def subjectMap = _subjectMap
  def triplesMap = _triplesMap
  def functionSubjectMap = _functionSubjectMap

  //setting predefined prefixes
  for(rdfNamespace <- RdfNamespace.prefixMap) {
    model.setNsPrefix(rdfNamespace._2.prefix, rdfNamespace._2.namespace)
  }

  private def convertToLogicalSourceUri(title: WikiTitle): String =
  {
    title.resourceIri + "/" + title.encoded.toString().trim + "/LogicalSource"
  }

  private def convertToSubjectMapUri(title: WikiTitle): String =
  {
    title.resourceIri + "/" + title.encoded.toString().trim + "/SubjectMap"
  }


}
