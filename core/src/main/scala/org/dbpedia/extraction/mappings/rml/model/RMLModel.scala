package org.dbpedia.extraction.mappings.rml.model

import org.apache.jena.rdf.model.{Property, Resource}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.mappings.rml.model.factory.RMLResourceFactory
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.wikiparser.WikiTitle
import collection.JavaConverters._

/**
  * ModelWrapper that is used for resembling RML Mappings
  */
abstract class RMLModel extends ModelWrapper {

  val rmlFactory = new RMLResourceFactory(model)

  protected val _triplesMap: RMLTriplesMap
  protected val _subjectMap: RMLSubjectMap
  protected val _logicalSource: RMLLogicalSource
  protected val _functionSubjectMap: RMLSubjectMap

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
    *
    * @param rmlUri
    * @return
    */
  def containsResource(rmlUri: RMLUri) : Boolean = {
    model.containsResource(model.createResource(rmlUri.toString))
  }

  protected def convertToLogicalSourceUri(title: WikiTitle): String =
  {
    title.resourceIri + "/LogicalSource"
  }

  protected def convertToSubjectMapUri(title: WikiTitle): String =
  {
    title.resourceIri + "/SubjectMap"
  }

  def count(templateName : String) : Int = {
    // define resource and property to look for
    val triplesMapResource = triplesMap.resource
    val tmrURI = triplesMap.resource.getURI
    val pomProperty = model.createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")

    // count the amount of statements that contain "SimplePropertyMapping"
    val statements = model.listStatements(triplesMapResource, pomProperty, null).toList.asScala
    statements.count(statement => statement.getObject.asResource().getURI.contains(templateName))
  }


}
