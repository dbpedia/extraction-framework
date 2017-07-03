package org.dbpedia.extraction.mappings.rml.translation.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.mappings.rml.translation.model.factories.RMLResourceFactory
import org.dbpedia.extraction.mappings.rml.translation.model.rmlresources._
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
                                                              .addClass(new RMLUri(RdfNamespace.FNO.namespace + "Execution"))
                                                                .addBlankNodeTermType()


  _logicalSource.addIterator(new RMLLiteral("Infobox"))
  _logicalSource.addReferenceFormulation(new RMLUri(RdfNamespace.QL.namespace + "wikitext"))

  def logicalSource = _logicalSource
  def subjectMap = _subjectMap
  def triplesMap = _triplesMap
  def functionSubjectMap = _functionSubjectMap

  //setting predefined prefixes
  for(rdfNamespace <- RdfNamespace.prefixMap) {
    model.setNsPrefix(rdfNamespace._2.prefix, rdfNamespace._2.namespace)
  }

  /**
    * Checks if the Jena model contains the given RMLUri as a resource
    * @param rmlUri
    * @return
    */
  def containsResource(rmlUri: RMLUri) : Boolean = {
    model.containsResource(model.createResource(rmlUri.toString))
  }

  private def convertToLogicalSourceUri(title: WikiTitle): String =
  {
    title.resourceIri + "/LogicalSource"
  }

  private def convertToSubjectMapUri(title: WikiTitle): String =
  {
    title.resourceIri + "/SubjectMap"
  }


}
