package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, StartDateTemplate, _}
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplates, TemplatesJsonConverter}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdTemplatesJsonConverter extends TemplatesJsonConverter {

  override def convertAll(templates: Set[Template]): JsonTemplates = {

    val array = JsonNodeFactory.instance.arrayNode()
    val node = JsonNodeFactory.instance.objectNode()
    node.set("templates", array)

    templates.foreach(template => {

      val converter = template match {

        case template : SimplePropertyTemplate => new StdSimplePropertyJsonConverter
        case template : ConstantTemplate => new StdConstantJsonConverter
        case template : GeocoordinateTemplate => new StdGeocoordinatesJsonConverter
        case template : IntermediateTemplate => new StdIntermediateJsonConverter
        case template : StartDateTemplate => new StdStartDateJsonConverter
        case template : EndDateTemplate => new StdEndDateJsonConverter
        case template : ConditionalTemplate => new StdConditionalJsonConverter
        case _ => null

      }

      if(converter != null) {
        val jsonTemplate = converter(template).asInstanceOf[StdJsonTemplate]
        array.add(jsonTemplate.node)
      }

    })

    new StdJsonTemplates(node)

  }

}
