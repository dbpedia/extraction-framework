package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.datatypes.Datatype

/**
  * Created by wmaroy on 24.07.17.
  */
case class SimplePropertyTemplate(property : String,
                                  ontologyProperty : OntologyProperty,
                                  select : String,
                                  prefix : String,
                                  suffix : String,
                                  transform : String,
                                  unit : Datatype,
                                  factor : Double = 1) extends Template(SimplePropertyTemplate.NAME) {

  def isSimple : Boolean = {
    select == null && prefix == null && transform == null && factor == 1
  }

}

object SimplePropertyTemplate {

  val NAME = "SimplePropertyTemplate"

}