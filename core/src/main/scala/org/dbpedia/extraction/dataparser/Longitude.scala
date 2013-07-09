package org.dbpedia.extraction.dataparser

/**
 *  Represents the longitude component of geographical coordinates.
 *
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class Longitude( lonDeg : Double = 0.0, lonMin : Double = 0.0, lonSec : Double = 0.0, lonHem : String = "E",
                     val belongsToArticle : Boolean = false )
extends SingleGeoCoordinate
{
    override def toDouble = (lonDeg + (lonMin + lonSec / 60.0 ) / 60.0) * (if(lonHem == "W" || lonHem == "O") -1.0 else 1.0)

    require(lonHem == "E" || lonHem == "W" || lonHem == "O", "Invalid hemisphere: '" + lonHem + "'")
    // TODO: remove lonHem == "O"
    require(toDouble >= -90.0 && toDouble <= 90.0, "Longitude must be in the range [-90, 90]")
}
