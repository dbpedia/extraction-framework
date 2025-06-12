package org.dbpedia.extraction.dataparser

/**
 * Represents a geographical location determined by latitude and longitude coordinates.
 *
 * @param latitude The latitude in decimal degrees
 * @param longitude The longitude in decimal degrees
 * @param belongsToArticle Whether this coordinate belongs to the main article
 * @throws IllegalArgumentException if the given parameters do not denote a valid coordinate
 */
class GeoCoordinate(
                     val latitude: Double,
                     val longitude: Double,
                     val belongsToArticle: Boolean = false
                   ) {

  // Validation
  if (latitude < -90.0 || latitude > 90.0) {
    throw new IllegalArgumentException(s"Invalid latitude: $latitude. Must be between -90 and 90 degrees.")
  }
  if (longitude < -180.0 || longitude > 180.0) {
    throw new IllegalArgumentException(s"Invalid longitude: $longitude. Must be between -180 and 180 degrees.")
  }

  /**
   * Constructor using Latitude and Longitude objects
   */
  def this(lat: Latitude, long: Longitude, belongsToArticle: Boolean) = {
    this(lat.toDouble, long.toDouble, belongsToArticle)
  }

  /**
   * Constructor with default coordinates (0,0)
   */
  def this(belongsToArticle: Boolean) = {
    this(0.0, 0.0, belongsToArticle)
  }

  /**
   * Constructor using DMS (Degrees, Minutes, Seconds) format
   */
  def this(
            latDeg: Double, latMin: Double, latSec: Double, latHem: String,
            lonDeg: Double, lonMin: Double, lonSec: Double, lonHem: String,
            belongsToArticle: Boolean
          ) = {
    this(
      new Latitude(latDeg, latMin, latSec, latHem, true),
      new Longitude(lonDeg, lonMin, lonSec, lonHem),
      belongsToArticle
    )
  }

  /**
   * Get latitude hemisphere (N/S)
   */
  def latitudeHemisphere: String = if (latitude >= 0) "N" else "S"

  /**
   * Get longitude hemisphere (E/W)
   */
  def longitudeHemisphere: String = if (longitude >= 0) "E" else "W"

  /**
   * Get absolute latitude value
   */
  def absoluteLatitude: Double = math.abs(latitude)

  /**
   * Get absolute longitude value
   */
  def absoluteLongitude: Double = math.abs(longitude)

  /**
   * Convert to DMS format for latitude
   */
  def latitudeToDMS: (Int, Int, Double, String) = {
    val absLat = absoluteLatitude
    val degrees = absLat.toInt
    val minutes = ((absLat - degrees) * 60).toInt
    val seconds = ((absLat - degrees) * 60 - minutes) * 60
    (degrees, minutes, seconds, latitudeHemisphere)
  }

  /**
   * Convert to DMS format for longitude
   */
  def longitudeToDMS: (Int, Int, Double, String) = {
    val absLon = absoluteLongitude
    val degrees = absLon.toInt
    val minutes = ((absLon - degrees) * 60).toInt
    val seconds = ((absLon - degrees) * 60 - minutes) * 60
    (degrees, minutes, seconds, longitudeHemisphere)
  }

  override def toString: String = {
    f"GeoCoordinate(${latitude}%.6f, ${longitude}%.6f, belongsToArticle=$belongsToArticle)"
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: GeoCoordinate =>
      math.abs(latitude - other.latitude) < 1e-6 &&
        math.abs(longitude - other.longitude) < 1e-6 &&
        belongsToArticle == other.belongsToArticle
    case _ => false
  }

  override def hashCode(): Int = {
    val prime = 31
    var result = 1
    result = prime * result + latitude.hashCode
    result = prime * result + longitude.hashCode
    result = prime * result + belongsToArticle.hashCode
    result
  }
}