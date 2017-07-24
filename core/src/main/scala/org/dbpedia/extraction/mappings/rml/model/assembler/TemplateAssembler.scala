package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.RMLTriplesMap
import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 24.07.17.
  * This object adds mapping templates from
  * the rml.model.template package to an RMLTriplesMap from the rml.model.resource package
  */
object TemplateAssembler {

  /**
    *
    * @param rmlModel
    * @param simplePropertyTemplate
    */
  def assembleSimplePropertyTemplate(rmlModel : RMLModel, simplePropertyTemplate: SimplePropertyTemplate, language: String) = {
    val assembler = new SimplePropertyTemplateAssembler(rmlModel, language, simplePropertyTemplate)
    assembler.assemble()
  }

}
