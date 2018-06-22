package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}
import org.dbpedia.extraction.mappings.rml.model.template.{ConstantTemplate, Template}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdConstantJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val constantTemplate = template.asInstanceOf[ConstantTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    parameters.put("value", constantTemplate.value)

    val ontologyProperty = if (constantTemplate.ontologyProperty != null) {
      constantTemplate.ontologyProperty.uri
    } else null
    parameters.put("ontologyProperty", ontologyProperty)

    val unit = if (constantTemplate.unit != null) {
      constantTemplate.unit.uri
    } else null
    parameters.put("unit", unit)

    new StdJsonTemplate(node)

  }

}
