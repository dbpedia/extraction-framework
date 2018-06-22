package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}
import org.dbpedia.extraction.mappings.rml.model.template.{IntermediateTemplate, Template}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdIntermediateJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val intermediateTemplate = template.asInstanceOf[IntermediateTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    val _class = if (intermediateTemplate.ontologyClass != null) {
      intermediateTemplate.ontologyClass.uri
    } else null
    parameters.put("class", _class)

    val ontologyProperty = if (intermediateTemplate.property != null) {
      intermediateTemplate.property.uri
    } else null
    parameters.put("property", ontologyProperty)

    val converter = new StdTemplatesJsonConverter
    val mapper = new ObjectMapper
    val nodeWithTemplates = mapper.readTree(converter.convertAll(intermediateTemplate.templates.toSet).toString)
    val templatesArrayNode = nodeWithTemplates.get("templates")
    node.set("templates", templatesArrayNode)

    new StdJsonTemplate(node)

  }

}
