package org.dbpedia.extraction.dataparser

/**
 *  RepresenRepresents the latitude component of geographical coordinates.
 *
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class Latitude( latDeg : Double = 0.0, latMin : Double = 0.0, latSec : Double = 0.0, latHem : String = "N",
                     val belongsToArticle : Boolean = false )
extends SingleGeoCoordinate
{
    override def toDouble = (latDeg + (latMin + latSec / 60.0 ) / 60.0) * (if(latHem == "S") -1.0 else 1.0)

    require(latHem == "N" || latHem == "S", "Invalid hemisphere: '" + latHem + "'")

    require(toDouble >= -90.0 && toDouble <= 90.0, "Latitude must be in the range [-90, 90]")
}
