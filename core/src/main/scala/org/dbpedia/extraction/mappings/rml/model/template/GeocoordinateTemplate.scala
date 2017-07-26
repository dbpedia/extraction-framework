package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty

/**
  * Created by wmaroy on 24.07.17.
  */
case class GeocoordinateTemplate(ontologyProperty: OntologyProperty,
                                 coordinate : String,
                                 latitude : String,
                                 longitude : String,
                                 latitudeDegrees : String,
                                 latitudeMinutes : String,
                                 latitudeSeconds : String,
                                 latitudeDirection: String,
                                 longitudeDegrees : String,
                                 longitudeMinutes : String,
                                 longitudeSeconds : String,
                                 longitudeDirection: String) extends Template
