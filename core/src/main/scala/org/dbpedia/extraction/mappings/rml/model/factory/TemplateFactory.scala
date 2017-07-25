package org.dbpedia.extraction.mappings.rml.model.factory

import org.dbpedia.extraction.mappings.rml.model.template.{SimplePropertyTemplate, Template}

/**
  * Created by wmaroy on 24.07.17.
  */
trait TemplateFactory {

  def createTemplate : Template

}
