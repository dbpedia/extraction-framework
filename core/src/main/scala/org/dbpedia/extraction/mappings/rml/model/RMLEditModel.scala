package org.dbpedia.extraction.mappings.rml.model
import org.apache.jena.rdf.model.Model
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLogicalSource, RMLSubjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.ontology.RdfNamespace

import collection.JavaConverters._

/**
  * Created by wmaroy on 21.07.17.
  */
class RMLEditModel(private val mapping : Model,
                   val name : String,
                   val base : String,
                   val language : String) extends RMLModel {



  // add the mapping to the core model
  model.add(mapping)

  override protected val _triplesMap: RMLTriplesMap = getMainTriplesMap
  override protected val _subjectMap: RMLSubjectMap = _triplesMap.addSubjectMap(new RMLUri(base + "SubjectMap"))
  override protected val _logicalSource: RMLLogicalSource = _triplesMap.addLogicalSource(new RMLUri(base + "LogicalSource"))
  override protected val _functionSubjectMap: RMLSubjectMap = rmlFactory.createRMLSubjectMap(new RMLUri(base + "SubjectMap/Function"))
                                                                        .addClass(new RMLUri(RdfNamespace.FNO.namespace + "Execution"))
                                                                        .addBlankNodeTermType()

  def countSimpleProperties : Int = {
    // define resource and property to look for
    val triplesMapResource = triplesMap.resource
    val tmrURI = triplesMap.resource.getURI
    val pomProperty = model.createProperty(RdfNamespace.RR.namespace + "predicateObjectMap")

    // count the amount of statements that contain "SimplePropertyMapping"
    val statements = model.listStatements(triplesMapResource, pomProperty, null).toList.asScala
    statements.count(statement => statement.getObject.asResource().getURI.contains("SimplePropertyMapping"))
  }

  private def getMainTriplesMap : RMLTriplesMap = {
    val triplesMapResourceIRI = base.substring(0, base.lastIndexOf('/'))
    val triplesMap = model.getResource(triplesMapResourceIRI)
    rmlFactory.createRMLTriplesMap(new RMLUri(triplesMap.getURI))
  }

  override def toString : String = {
    "RML Mapping:\n" +
    "Name: " + name + "\n" +
    "Language: " + language + "\n"
  }

}
