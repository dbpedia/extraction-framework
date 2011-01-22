package org.dbpedia.extraction.dataparser

/**
 *  Represents a geographical location determined by latitude and longitude coordinates.
 *
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class GeoCoordinate( latDeg : Double = 0.0, latMin : Double = 0.0, latSec : Double = 0.0, latHem : String = "N",
                     lonDeg : Double = 0.0, lonMin : Double = 0.0, lonSec : Double = 0.0, lonHem : String = "E",
                     val belongsToArticle : Boolean = false )
{
    val latitude = (latDeg + (latMin + latSec / 60.0 ) / 60.0) * (if(latHem == "S") -1.0 else 1.0)
    val longitude = (lonDeg + (lonMin + lonSec / 60.0 ) / 60.0) * (if(lonHem == "W") -1.0 else 1.0)

    require(latHem == "N" || latHem == "S", "Invalid hemisphere: '" + latHem + "'")
    require(lonHem == "W" || lonHem == "O" || lonHem == "E", "Invalid hemisphere: '" + lonHem + "'")
    require(latitude >= -90.0 && latitude <= 90.0, "Latitude must be in the range [-90, 90]")
    require(longitude >= -180 && longitude <= 180.0, "Longitude must be in the range [-180, 180]")
}
