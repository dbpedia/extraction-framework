package org.dbpedia.extraction.dataparser

/**
 *  Represents a geographical location determined by latitude and longitude coordinates.
 *
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class GeoCoordinate( val latitude : Double,
                     val longitude : Double,
                     val belongsToArticle : Boolean = false )
{
  def this( lat : Latitude,
            long : Longitude,
            belongsToArticle : Boolean
            )= this( lat.toDouble, long.toDouble, belongsToArticle)
  def this(
      latDeg : Double = 0.0, latMin : Double = 0.0, latSec : Double = 0.0, latHem : String = "N",
      lonDeg : Double = 0.0, lonMin : Double = 0.0, lonSec : Double = 0.0, lonHem : String = "E",
      belongsToArticle : Boolean
        )= this(
            new Latitude(latDeg, latMin, latSec, latHem, true),
            new Longitude(lonDeg, lonMin, lonSec, lonHem),
            belongsToArticle )

}
