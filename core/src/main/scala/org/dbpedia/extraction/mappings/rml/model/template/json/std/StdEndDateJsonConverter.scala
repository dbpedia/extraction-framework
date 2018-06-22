package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, Template}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdEndDateJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val edTemplate = template.asInstanceOf[EndDateTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    val ontologyProperty = if (edTemplate.ontologyProperty != null) {
      edTemplate.ontologyProperty.uri
    } else null
    parameters.put("ontologyProperty", ontologyProperty)
    parameters.put("property", edTemplate.property)

    new StdJsonTemplate(node)

  }

}
