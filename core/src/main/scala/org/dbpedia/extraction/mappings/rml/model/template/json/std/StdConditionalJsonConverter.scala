package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}
import org.dbpedia.extraction.mappings.rml.model.template.{ConditionalTemplate, Template}

/**
  * Created by wmaroy on 14.08.17.
  */
class StdConditionalJsonConverter extends TemplateJsonConverter {

  def apply(template: Template): JsonTemplate = {

    if (!template.isInstanceOf[ConditionalTemplate]) throw new IllegalArgumentException("Template instance is not of class ConditionalTemplate.")

    val condTemplate = template.asInstanceOf[ConditionalTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    val conditionNode = if (condTemplate.condition != null) {
      val conditionNode = JsonNodeFactory.instance.objectNode()
      conditionNode.put("operator", condTemplate.condition.operator)

      val conditionParameterNode = JsonNodeFactory.instance.objectNode()
      conditionNode.set("parameters", conditionParameterNode)

      conditionParameterNode.put("property", condTemplate.condition.property)
      conditionParameterNode.put("value", condTemplate.condition.value)

      conditionNode

    } else null

    parameters.set("condition", conditionNode)

    // it is allowed to be null
    if (condTemplate.ontologyClass != null) {
      parameters.put("class", condTemplate.ontologyClass.uri)
    }

    val converter = new StdTemplatesJsonConverter
    val mapper = new ObjectMapper
    val nodeWithTemplates = mapper.readTree(converter.convertAll(condTemplate.templates.toSet).toString)
    val templatesArrayNode = nodeWithTemplates.get("templates")

    parameters.set("templates", templatesArrayNode)

    val fallbackNode = if (condTemplate.hasFallback) {
      val converter = new StdConditionalJsonConverter
      val convertedFallback = converter(condTemplate.fallback)
      mapper.readTree(convertedFallback.toString)
    } else null

    parameters.set("fallback", fallbackNode)

    new StdJsonTemplate(node)

  }
}
