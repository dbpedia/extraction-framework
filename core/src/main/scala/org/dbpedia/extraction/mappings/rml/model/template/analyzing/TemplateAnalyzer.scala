package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.RMLModel

/**
  * Created by wmaroy on 11.08.17.
  */
trait TemplateAnalyzer {

  def analyze(model : RMLModel) : AnalyzedRMLModel

}
