package org.dbpedia.extraction.dataparser

/**
 *  Represents a geographical location determined by latitude and longitude coordinates.
 *
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class GeoCoordinate( lat : Latitude,
                     long : Longitude,
                     val belongsToArticle : Boolean = false )
{
  def this(
      latDeg : Double = 0.0, latMin : Double = 0.0, latSec : Double = 0.0, latHem : String = "N",
      lonDeg : Double = 0.0, lonMin : Double = 0.0, lonSec : Double = 0.0, lonHem : String = "E",
      belongsToArticle : Boolean = false
        )= this(
            new Latitude(latDeg, latMin, latSec, latHem, true), 
            new Longitude(lonDeg, lonMin, lonSec, lonHem),
            belongsToArticle
            )
            
   val latitude = lat.toDouble
   val longitude = long.toDouble
            
}
