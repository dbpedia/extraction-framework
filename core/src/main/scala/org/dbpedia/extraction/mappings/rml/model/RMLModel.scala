package org.dbpedia.extraction.mappings.rml.model
import org.apache.jena.rdf.model.Model
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLogicalSource, RMLSubjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.ontology.RdfNamespace



/**
  * Created by wmaroy on 21.07.17.
  */
class RMLModel(private val mapping : Model,
               val name : String,
               val base : String,
               val language : String) extends AbstractRMLModel {



  // add the mapping to the core model, if null: this will be a fresh mapping
  if(model != null) {
    model.add(mapping)
  } else {
    val triplesMapResourceIRI = RMLUri(base.substring(0, base.lastIndexOf('/')))
    rmlFactory.createRMLTriplesMap(triplesMapResourceIRI)
  }

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

object RMLModel {

  def createBase(templateTitle : String, language : String) : String = {
    "http://" + language + ".dbpedia.org/resource/Mapping_" + language + ":" + templateTitle + "/"
  }

  def createName(templateTitle : String, language : String) : String = {
    "Mapping_" + language + ":" + templateTitle
  }

}
