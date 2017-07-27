package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty

/**
  * Created by wmaroy on 24.07.17.
  */
case class StartDateTemplate(property : String, ontologyProperty : OntologyProperty) extends Template(StartDateTemplate.NAME)

object StartDateTemplate {

  val NAME = "StartDateTemplate"

}