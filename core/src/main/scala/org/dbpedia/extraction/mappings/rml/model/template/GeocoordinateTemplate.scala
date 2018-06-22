package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty

/**
  * Created by wmaroy on 24.07.17.
  */
case class GeocoordinateTemplate(ontologyProperty: OntologyProperty,
                                 coordinate: String,
                                 latitude: String,
                                 longitude: String,
                                 latitudeDegrees: String,
                                 latitudeMinutes: String,
                                 latitudeSeconds: String,
                                 latitudeDirection: String,
                                 longitudeDegrees: String,
                                 longitudeMinutes: String,
                                 longitudeSeconds: String,
                                 longitudeDirection: String) extends Template(GeocoordinateTemplate.NAME)

object GeocoordinateTemplate {

  def apply(lat: LatitudeTemplate, lon: LongitudeTemplate, ontologyProperty: OntologyProperty = null): GeocoordinateTemplate = {
    GeocoordinateTemplate(ontologyProperty,
      lon.coordinates,
      lat.latitude, lon.longitude,
      lat.degrees, lat.minutes, lat.seconds, lat.direction,
      lon.degrees, lon.minutes, lon.seconds, lon.direction)
  }

  val NAME = "GeocoordinateTemplate"

}
