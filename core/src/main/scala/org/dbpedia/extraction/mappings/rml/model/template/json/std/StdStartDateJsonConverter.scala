package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}
import org.dbpedia.extraction.mappings.rml.model.template.{StartDateTemplate, Template}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdStartDateJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val sdTemplate = template.asInstanceOf[StartDateTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    val ontologyProperty = if (sdTemplate.ontologyProperty != null) {
      sdTemplate.ontologyProperty.uri
    } else null
    parameters.put("ontologyProperty", sdTemplate.ontologyProperty.uri)

    parameters.put("property", sdTemplate.property)

    new StdJsonTemplate(node)

  }

}
