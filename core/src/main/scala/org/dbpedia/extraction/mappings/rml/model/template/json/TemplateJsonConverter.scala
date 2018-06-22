package org.dbpedia.extraction.mappings.rml.model.template.json

import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 12.08.17.
  */
trait TemplateJsonConverter {

  def apply(template: Template): JsonTemplate

}
