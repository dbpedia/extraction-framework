package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.{OntologyClass, OntologyProperty}

/**
  * Created by wmaroy on 24.07.17.
  */
case class IntermediateTemplate(ontologyClass: OntologyClass, property : OntologyProperty, templates : List[Template]) extends Template(IntermediateTemplate.NAME)

object IntermediateTemplate {

  val NAME = "IntermediateTemplate"

}