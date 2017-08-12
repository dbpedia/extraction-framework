package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.{SimplePropertyTemplate, Template}
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdSimplePropertyJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val spTemplate = template.asInstanceOf[SimplePropertyTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    parameters.put("property", spTemplate.property)

    val ontologyProperty = if(spTemplate.ontologyProperty != null) {
      spTemplate.ontologyProperty.uri
    } else null
    parameters.put("ontologyProperty", ontologyProperty)

    parameters.put("factor", spTemplate.factor)
    parameters.put("select", spTemplate.select)
    parameters.put("transform", spTemplate.transform)
    parameters.put("prefix", spTemplate.prefix)
    parameters.put("suffix", spTemplate.suffix)

    val unit = if(spTemplate.unit != null) {
      spTemplate.unit.uri
    } else null
    parameters.put("unit", spTemplate.unit.uri)

    new StdJsonTemplate(node)

  }

}
