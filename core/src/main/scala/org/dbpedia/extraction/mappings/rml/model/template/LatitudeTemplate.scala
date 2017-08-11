package org.dbpedia.extraction.mappings.rml.model.template

/**
  * Created by wmaroy on 11.08.17.
  */
case class LatitudeTemplate(coordinates : String,
                            latitude : String,
                            latitudeDegrees : String,
                            latitudeMinutes : String,
                            latitudeSeconds : String,
                            latitudeDirection: String) extends Template(LatitudeTemplate.NAME){



}

object LatitudeTemplate {

  val NAME = "LatitudeTemplate"

}
