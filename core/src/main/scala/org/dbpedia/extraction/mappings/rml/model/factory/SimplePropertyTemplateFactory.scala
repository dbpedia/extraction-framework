package org.dbpedia.extraction.mappings.rml.model.factory

import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate

/**
  * Created by wmaroy on 24.07.17.
  */
trait SimplePropertyTemplateFactory {

  def createTemplate : SimplePropertyTemplate

}
