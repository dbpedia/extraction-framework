package org.dbpedia.extraction.mappings.rml.translate.format

import org.dbpedia.extraction.mappings.rml.model.AbstractRMLModel

/**
  * Created by wmaroy on 03.07.17.
  *
  * Formats a model
  *
  */
trait Formatter {

  def format(model: AbstractRMLModel, base: String): String

}
