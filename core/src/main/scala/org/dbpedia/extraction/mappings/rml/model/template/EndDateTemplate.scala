package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty

/**
  * Created by wmaroy on 24.07.17.
  */
case class EndDateTemplate(property: String, ontologyProperty: OntologyProperty) extends Template(EndDateTemplate.NAME)

object EndDateTemplate {

  val NAME = "EndDateTemplate"

}
