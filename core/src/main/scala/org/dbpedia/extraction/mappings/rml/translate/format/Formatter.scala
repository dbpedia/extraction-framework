package org.dbpedia.extraction.mappings.rml.translate.format

import org.dbpedia.extraction.mappings.rml.model.RMLModel

/**
  * Created by wmaroy on 03.07.17.
  *
  * Formats a model
  *
  */
trait Formatter {

  def format(model : RMLModel, base : String) : String

}
