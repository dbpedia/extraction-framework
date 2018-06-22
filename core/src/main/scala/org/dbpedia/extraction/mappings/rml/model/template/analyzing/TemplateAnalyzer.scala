package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.resource.RMLPredicateObjectMap
import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 11.08.17.
  */
trait TemplateAnalyzer {

  def apply(pom: RMLPredicateObjectMap): Template

}
