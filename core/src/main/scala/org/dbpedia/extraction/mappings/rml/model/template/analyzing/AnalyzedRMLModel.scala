package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 11.08.17.
  */
class AnalyzedRMLModel(rmlModel: RMLModel, val templates: Set[Template]) extends RMLModel(rmlModel.model,
  rmlModel.name,
  rmlModel.base,
  rmlModel.language) {


}


