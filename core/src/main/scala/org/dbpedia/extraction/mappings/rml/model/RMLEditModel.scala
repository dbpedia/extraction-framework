package org.dbpedia.extraction.mappings.rml.model
import org.apache.jena.rdf.model.Model
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLogicalSource, RMLSubjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.ontology.RdfNamespace



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
  override protected val _subjectMap: RMLSubjectMap = _triplesMap.addSubjectMap(RMLUri(base + "SubjectMap"))
  override protected val _logicalSource: RMLLogicalSource = _triplesMap.addLogicalSource(RMLUri(base + "LogicalSource"))
  override protected val _functionSubjectMap: RMLSubjectMap = rmlFactory.createRMLSubjectMap(RMLUri(base + "SubjectMap/Function"))
                                                                        .addClass(RMLUri(RdfNamespace.FNO.namespace + "Execution"))
                                                                        .addBlankNodeTermType()

  private def getMainTriplesMap : RMLTriplesMap = {
    val triplesMapResourceIRI = base.substring(0, base.lastIndexOf('/'))
    val triplesMap = model.getResource(triplesMapResourceIRI)
    rmlFactory.createRMLTriplesMap(RMLUri(triplesMap.getURI))
  }

  override def toString : String = {
    "RML Mapping:\n" +
    "Name: " + name + "\n" +
    "Language: " + language + "\n"
  }

}
