package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.datatypes.Datatype

/**
  * Created by wmaroy on 24.07.17.
  */
class ConstantTemplate(val ontologyProperty: OntologyProperty,
                       val value: String,
                       val unit: Datatype) extends Template(ConstantTemplate.NAME)

object ConstantTemplate {

  val NAME = "ConstantTemplate"

}
