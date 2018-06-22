package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLTriplesMap}
import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 11.08.17.
  */
trait TemplatesAnalyzer {

  def analyze(tm: RMLTriplesMap): Set[Template]

  def analyze(pom: RMLPredicateObjectMap): Template

}
