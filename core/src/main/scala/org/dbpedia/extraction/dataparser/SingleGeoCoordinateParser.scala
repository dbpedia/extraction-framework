package org.dbpedia.extraction.dataparser

import java.util.logging.{Level, Logger}
import util.control.ControlThrowable
import org.dbpedia.extraction.wikiparser.{TemplateNode, Node}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig
import org.dbpedia.extraction.mappings.Redirects
import scala.language.reflectiveCalls
import scala.util.matching.Regex

/**
 * Parses a single geographical coordinate, ie. either a latitude or a longitude.
 */
class SingleGeoCoordinateParser(context: {def language: Language}) extends DataParser {

  @transient private val logger = Logger.getLogger(classOf[SingleGeoCoordinateParser].getName)
  private val language = context.language.wikiCode

  private val lonHemLetterMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.longitudeLetterMap("en"))
  private val latHemLetterMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.latitudeLetterMap("en"))

  private val lonHemRegex = lonHemLetterMap.keySet.mkString("|")
  private val latHemRegex = latHemLetterMap.keySet.mkString("|")

  // Enhanced regex patterns for different coordinate formats
  private val LongitudeDMSRegex = ("""([0-9]{1,3})[°º]\s*([0-9]{1,2})['′]\s*([0-9]{1,2}(?:\.[0-9]+)?)[″"]\s*(""" + lonHemRegex + """)""").r
  private val LongitudeDMRegex = ("""([0-9]{1,3})[°º]\s*([0-9]{1,2}(?:\.[0-9]+)?)['′]\s*(""" + lonHemRegex + """)""").r
  private val LongitudeDRegex = ("""([0-9]{1,3}(?:\.[0-9]+)?)[°º]\s*(""" + lonHemRegex + """)""").r
  private val LongitudeSlashRegex = ("""([0-9]{1,3})/([0-9]{1,2})/([0-9]{0,2}(?:\.[0-9]+)?)[/]?\s*(""" + lonHemRegex + """)""").r
  private val LongitudeDecimalRegex = ("""(-?[0-9]{1,3}(?:\.[0-9]+)?)""").r

  private val LatitudeDMSRegex = ("""([0-9]{1,2})[°º]\s*([0-9]{1,2})['′]\s*([0-9]{1,2}(?:\.[0-9]+)?)[″"]\s*(""" + latHemRegex + """)""").r
  private val LatitudeDMRegex = ("""([0-9]{1,2})[°º]\s*([0-9]{1,2}(?:\.[0-9]+)?)['′]\s*(""" + latHemRegex + """)""").r
  private val LatitudeDRegex = ("""([0-9]{1,2}(?:\.[0-9]+)?)[°º]\s*(""" + latHemRegex + """)""").r
  private val LatitudeSlashRegex = ("""([0-9]{1,2})/([0-9]{1,2})/([0-9]{0,2}(?:\.[0-9]+)?)[/]?\s*(""" + latHemRegex + """)""").r
  private val LatitudeDecimalRegex = ("""(-?[0-9]{1,2}(?:\.[0-9]+)?)""").r

  override def parse(node: Node): Option[ParseResult[SingleGeoCoordinate]] = {
    try {
      for (text <- StringParser.parse(node);
           coordinate <- parseSingleCoordinate(text.value)) {
        return Some(ParseResult(coordinate))
      }
    }
    catch {
      case ex: ControlThrowable => throw ex
      case ex: Exception =>
        logger.log(Level.FINE, "Could not extract single coordinate", ex)
    }
    None
  }

  /**
   * Parse a single coordinate string into a SingleGeoCoordinate
   */
  def parseSingleCoordinate(coordStr: String): Option[SingleGeoCoordinate] = {
    val cleanCoordStr = coordStr.trim.replace("″", "\"").replace("′", "'")

    // Try latitude patterns first
    cleanCoordStr match {
      // DMS format: 51°30'25.5"N
      case LatitudeDMSRegex(deg, min, sec, hem) =>
        val hemisphere = latHemLetterMap.getOrElse(hem, hem)
        Some(new Latitude(deg.toDouble, min.toDouble, sec.toDouble, hemisphere))

      // DM format: 51°30.425'N
      case LatitudeDMRegex(deg, min, hem) =>
        val hemisphere = latHemLetterMap.getOrElse(hem, hem)
        Some(new Latitude(deg.toDouble, min.toDouble, 0.0, hemisphere))

      // D format: 51.507°N
      case LatitudeDRegex(deg, hem) =>
        val hemisphere = latHemLetterMap.getOrElse(hem, hem)
        Some(new Latitude(deg.toDouble, 0.0, 0.0, hemisphere))

      // Slash format: 51/30/25.5/N
      case LatitudeSlashRegex(deg, min, sec, hem) =>
        val hemisphere = latHemLetterMap.getOrElse(hem, hem)
        val seconds = if (sec.isEmpty) 0.0 else sec.toDouble
        Some(new Latitude(deg.toDouble, min.toDouble, seconds, hemisphere))

      // Try longitude patterns
      case LongitudeDMSRegex(deg, min, sec, hem) =>
        val hemisphere = lonHemLetterMap.getOrElse(hem, hem)
        Some(new Longitude(deg.toDouble, min.toDouble, sec.toDouble, hemisphere))

      case LongitudeDMRegex(deg, min, hem) =>
        val hemisphere = lonHemLetterMap.getOrElse(hem, hem)
        Some(new Longitude(deg.toDouble, min.toDouble, 0.0, hemisphere))

      case LongitudeDRegex(deg, hem) =>
        val hemisphere = lonHemLetterMap.getOrElse(hem, hem)
        Some(new Longitude(deg.toDouble, 0.0, 0.0, hemisphere))

      case LongitudeSlashRegex(deg, min, sec, hem) =>
        val hemisphere = lonHemLetterMap.getOrElse(hem, hem)
        val seconds = if (sec.isEmpty) 0.0 else sec.toDouble
        Some(new Longitude(deg.toDouble, min.toDouble, seconds, hemisphere))

      // Try decimal formats (without hemisphere indicators)
      case LatitudeDecimalRegex(decimal) =>
        val value = decimal.toDouble
        if (value >= -90 && value <= 90) {
          val hemisphere = if (value >= 0) "N" else "S"
          Some(new Latitude(math.abs(value), 0.0, 0.0, hemisphere))
        } else None

      case LongitudeDecimalRegex(decimal) =>
        val value = decimal.toDouble
        if (value >= -180 && value <= 180) {
          val hemisphere = if (value >= 0) "E" else "W"
          Some(new Longitude(math.abs(value), 0.0, 0.0, hemisphere))
        } else None

      case _ => None
    }
  }

  /**
   * Determine if a coordinate string is more likely to be latitude or longitude
   */
  def isLikelyLatitude(coordStr: String): Boolean = {
    // Check for latitude-specific patterns
    latHemRegex.r.findFirstIn(coordStr).isDefined ||
      (coordStr.matches(""".*\d+.*""") && {
        val numbers = """\d+(?:\.\d+)?""".r.findAllIn(coordStr).map(_.toDouble).toList
        numbers.headOption.exists(n => n <= 90) // Latitude range
      })
  }

  /**
   * Determine if a coordinate string is more likely to be longitude
   */
  def isLikelyLongitude(coordStr: String): Boolean = {
    lonHemRegex.r.findFirstIn(coordStr).isDefined ||
      (coordStr.matches(""".*\d+.*""") && {
        val numbers = """\d+(?:\.\d+)?""".r.findAllIn(coordStr).map(_.toDouble).toList
        numbers.headOption.exists(n => n > 90 && n <= 180) // Longitude range
      })
  }
}
