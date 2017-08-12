package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.dbpedia.extraction.mappings.rml.model.template.{GeocoordinateTemplate, StartDateTemplate, Template}
import org.dbpedia.extraction.mappings.rml.model.template.json.{JsonTemplate, TemplateJsonConverter}

/**
  * Created by wmaroy on 12.08.17.
  */
class StdGeocoordinatesJsonConverter extends TemplateJsonConverter {

  override def apply(template: Template): JsonTemplate = {

    val geoTemplate = template.asInstanceOf[GeocoordinateTemplate]

    val node = JsonNodeFactory.instance.objectNode()
    node.put("name", template.name)

    val parameters = JsonNodeFactory.instance.objectNode()
    node.set("parameters", parameters)

    parameters.put("coordinate", geoTemplate.coordinate)

    val ontologyProperty = if(geoTemplate.ontologyProperty != null) {
      geoTemplate.ontologyProperty.uri
    } else null
    parameters.put("ontologyProperty" , ontologyProperty)

    parameters.put("latitude", geoTemplate.latitude)
    parameters.put("longitude", geoTemplate.longitude)

    parameters.put("latitudeDegrees", geoTemplate.latitudeDegrees)
    parameters.put("latitudeMinutes", geoTemplate.latitudeMinutes)
    parameters.put("latitudeSeconds", geoTemplate.latitudeSeconds)
    parameters.put("latitudeDirection", geoTemplate.latitudeDirection)

    parameters.put("longitudeDegrees", geoTemplate.longitudeDegrees)
    parameters.put("longitudeMinutes", geoTemplate.longitudeMinutes)
    parameters.put("longitudeSeconds", geoTemplate.longitudeSeconds)
    parameters.put("longitudeDirection", geoTemplate.longitudeDirection)

    new StdJsonTemplate(node)

  }

}
