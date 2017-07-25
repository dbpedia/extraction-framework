package org.dbpedia.extraction.mappings.rml.model.factory
import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.ConstantTemplate
import org.dbpedia.extraction.mappings.rml.util.JSONFactoryUtil
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 25.07.17.
  */
class ConstantTemplateJSONFactory(val templateNode: JsonNode, ontology: Ontology) extends TemplateFactory {

  private lazy val ontologyProperty = JSONFactoryUtil.getOntologyProperty(templateNode, ontology)
  private lazy val value = JSONFactoryUtil.parameters("value", templateNode)
  private lazy val unit = JSONFactoryUtil.getUnit(templateNode, ontology)

  override def createTemplate: ConstantTemplate = {
    new ConstantTemplate(ontologyProperty, value, unit)
  }

}
