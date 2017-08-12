package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.resource.RMLPredicateObjectMap
import org.dbpedia.extraction.mappings.rml.model.template.Template
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 12.08.17.
  */
class ConditionalTemplateAnalyzer(ontology: Ontology) extends AbstractTemplateAnalyzer(ontology) {

  def apply(pom: RMLPredicateObjectMap): Template = ???


}
