package org.dbpedia.extraction.mappings.rml.model.template

/**
  * Created by wmaroy on 11.08.17.
  */
case class LongitudeTemplate(coordinates: String,
                             longitude: String,
                             degrees: String,
                             minutes: String,
                             seconds: String,
                             direction: String) extends Template(LongitudeTemplate.NAME) {

  val kind = if (coordinates != null) {
    LatitudeTemplate.TYPE_1
  } else if (longitude != null) {
    LatitudeTemplate.TYPE_2
  } else {
    LatitudeTemplate.TYPE_3
  }

}

object LongitudeTemplate {

  val NAME = "LongitudeTemplate"

  val TYPE_1 = "Type 1"

  val TYPE_2 = "Type 2"

  val TYPE_3 = "Type 3"


}
