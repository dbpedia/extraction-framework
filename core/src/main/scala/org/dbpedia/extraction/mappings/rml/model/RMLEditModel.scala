package org.dbpedia.extraction.mappings.rml.model
import org.apache.jena.rdf.model.Model
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLogicalSource, RMLSubjectMap, RMLTriplesMap}
import org.dbpedia.extraction.ontology.RdfNamespace
import collection.JavaConverters._

/**
  * Created by wmaroy on 21.07.17.
  */
class RMLEditModel(private val mapping : Model) extends RMLModel {

  // add the mapping to the core model
  model.add(mapping)

  override protected val _triplesMap: RMLTriplesMap = null
  override protected val _subjectMap: RMLSubjectMap = null
  override protected val _functionSubjectMap: RMLSubjectMap = null
  override protected val _logicalSource: RMLLogicalSource = null

  private def getMainTriplesMap : RMLTriplesMap = {
      val triplesMaps = model.listResourcesWithProperty(model.getProperty(RdfNamespace.RDF.namespace + "type"),
        model.getResource(RdfNamespace.RR.namespace + "TriplesMap")).toList
      null
  }

}
