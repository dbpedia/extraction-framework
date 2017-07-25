package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.RMLTriplesMap
import org.dbpedia.extraction.mappings.rml.model.template.{ConstantTemplate, SimplePropertyTemplate}
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
    * @param language
    * @param counter
    * @return
    */
  def assembleSimplePropertyTemplate(rmlModel : RMLModel, simplePropertyTemplate: SimplePropertyTemplate, language: String, counter : Int) = {
    val assembler = new SimplePropertyTemplateAssembler(rmlModel, language, simplePropertyTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rmlModel
    * @param constantTemplate
    * @param language
    * @param counter
    * @return
    */
  def assembleConstantTemplate(rmlModel: RMLModel, constantTemplate : ConstantTemplate, language: String, counter : Int) = {
    val assembler = new ConstantTemplateAssembler(rmlModel, language, constantTemplate, counter)
    assembler.assemble()
  }

}
