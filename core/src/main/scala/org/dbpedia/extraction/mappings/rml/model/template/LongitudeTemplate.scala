package org.dbpedia.extraction.mappings.rml.model.template

/**
  * Created by wmaroy on 11.08.17.
  */
case class LongitudeTemplate(coordinates : String,
                        longitude : String,
                        degrees: String,
                        minutes : String,
                        seconds : String,
                        direction: String) extends Template(LongitudeTemplate.NAME) {

}

object LongitudeTemplate {

  val NAME = "LongitudeTemplate"

}
