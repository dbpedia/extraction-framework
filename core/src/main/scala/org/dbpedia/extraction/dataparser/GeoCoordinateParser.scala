package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{TemplateNode, Node}
import java.util.logging.{Level, Logger}
import util.control.ControlThrowable
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig
import org.dbpedia.extraction.mappings.Redirects
import scala.language.reflectiveCalls

/**
 * Parses geographical coordinates.
 */
class GeoCoordinateParser(
                           extractionContext: {
                             def language: Language
                             def redirects: Redirects
                           }
                         ) extends DataParser {

  private val templateNames = GeoCoordinateParserConfig.coordTemplateNames.map(_.toLowerCase)

  @transient private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)

  private val singleCoordParser = new SingleGeoCoordinateParser(extractionContext)

  private val language = extractionContext.language.wikiCode

  private val lonHemLetterMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.longitudeLetterMap("en"))
  private val latHemLetterMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.latitudeLetterMap("en"))
  private val lonHemRegex = lonHemLetterMap.keySet.mkString("|")
  private val latHemRegex = latHemLetterMap.keySet.mkString("|")

  private val Coordinate = ("""([0-9]{1,2})º([0-9]{1,2})\'([0-9]{1,2}(?:\.[0-9]{1,2})?)?\"?[\s]?(""" + latHemRegex + """)[\s]([0-9]{1,3})º([0-9]{1,2})\'([0-9]{1,2}(?:\.[0-9]{1,2})?)?\"?[\s]?(""" + lonHemRegex + """)""").r
  private val LatDir = ("""(""" + latHemRegex + """)""").r
  private val LonDir = ("""(""" + lonHemRegex + """)""").r

  override def parse(node: Node): Option[ParseResult[GeoCoordinate]] = {
    try {
      // Try to parse from template nodes first
      catchTemplate(node) match {
        case Some(coordinate) => return Some(ParseResult(coordinate))
        case None =>
          // fallback to parsing text
          StringParser.parse(node) match {
            case Some(text) =>
              parseGeoCoordinate(text.value) match {
                case Some(coord) => return Some(ParseResult(coord))
                case None => // continue
              }
            case None => // continue
          }
      }
    } catch {
      case ex: ControlThrowable => throw ex
      case ex: Exception =>
        logger.log(Level.FINE, "Could not extract coordinates", ex)
    }
    None
  }

  private def catchTemplate(node: Node): Option[GeoCoordinate] = {
    node match {
      case templateNode: TemplateNode
        if templateNames.contains(extractionContext.redirects.resolve(templateNode.title).decoded.toLowerCase) =>
        catchCoordTemplate(templateNode)
      case _ =>
        node.children.view.flatMap(catchTemplate).headOption
    }
  }

  private def toDoubleSafe(s: String): Option[Double] = try {
    Some(s.toDouble)
  } catch {
    case _: NumberFormatException => None
  }

  private def isHemisphere(s: String): Boolean = {
    val upper = s.toUpperCase
    upper == "N" || upper == "S" || upper == "E" || upper == "W"
  }

  private def convertDMSToDecimal(degrees: Double, minutes: Double, seconds: Double): Double = {
    degrees + minutes / 60.0 + seconds / 3600.0
  }

  private def isValidCoordinateComponent(value: Double, isMinutesOrSeconds: Boolean): Boolean = {
    if (isMinutesOrSeconds) {
      value >= 0.0 && value < 60.0
    } else {
      true // degrees can be any value, validation happens at coordinate level
    }
  }

  private def isValidCoordinate(lat: Double, lon: Double): Boolean = {
    lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0
  }

  private def catchCoordTemplate(node: TemplateNode): Option[GeoCoordinate] = {
    val belongsToArticle = node.property("display").toList.flatMap(displayNode =>
      displayNode.retrieveText.toList.flatMap(text =>
        text.split(",")
      )
    ).exists(option =>
      option == "t" || option == "title")

    val properties = node.children.flatMap(property => property.retrieveText).filter(_.trim.nonEmpty)

    // Filter out coordinate and template parameters (anything that starts with display=, type=, etc.)
    val coords = properties.filterNot(p => p.contains("=") || p.contains("display") || p.contains("type") || p.contains("region"))

    coords match {
      // Case 1: {{coord|dd|N/S|dd|E/W}} - Degrees only with hemispheres
      case latDeg :: latHem :: lonDeg :: lonHem :: _ if coords.length >= 4 && isHemisphere(latHem) && isHemisphere(lonHem) =>
        for {
          latD <- toDoubleSafe(latDeg)
          lonD <- toDoubleSafe(lonDeg)
          latSigned = if (latHem.toUpperCase == "S") -math.abs(latD) else math.abs(latD)
          lonSigned = if (lonHem.toUpperCase == "W") -math.abs(lonD) else math.abs(lonD)
          if isValidCoordinate(latSigned, lonSigned)
        } yield new GeoCoordinate(latSigned, lonSigned, belongsToArticle)

      // Case 2: {{coord|dd|mm|N/S|dd|mm|E/W}} - Degrees and minutes with hemispheres
      case latDeg :: latMin :: latHem :: lonDeg :: lonMin :: lonHem :: _ if coords.length >= 6 && isHemisphere(latHem) && isHemisphere(lonHem) =>
        for {
          latD <- toDoubleSafe(latDeg)
          latM <- toDoubleSafe(latMin)
          lonD <- toDoubleSafe(lonDeg)
          lonM <- toDoubleSafe(lonMin)
          if isValidCoordinateComponent(latM, true) && isValidCoordinateComponent(lonM, true)
          latDecimal = convertDMSToDecimal(math.abs(latD), latM, 0.0)
          lonDecimal = convertDMSToDecimal(math.abs(lonD), lonM, 0.0)
          latSigned = if (latHem.toUpperCase == "S") -latDecimal else latDecimal
          lonSigned = if (lonHem.toUpperCase == "W") -lonDecimal else lonDecimal
          if isValidCoordinate(latSigned, lonSigned)
        } yield new GeoCoordinate(latSigned, lonSigned, belongsToArticle)

      // Case 3: {{coord|dd|mm|ss|N/S|dd|mm|ss|E/W}} - Degrees, minutes, seconds with hemispheres
      case latDeg :: latMin :: latSec :: latHem :: lonDeg :: lonMin :: lonSec :: lonHem :: _ if coords.length >= 8 && isHemisphere(latHem) && isHemisphere(lonHem) =>
        for {
          latD <- toDoubleSafe(latDeg)
          latM <- toDoubleSafe(latMin)
          latS <- toDoubleSafe(latSec)
          lonD <- toDoubleSafe(lonDeg)
          lonM <- toDoubleSafe(lonMin)
          lonS <- toDoubleSafe(lonSec)
          if isValidCoordinateComponent(latM, true) && isValidCoordinateComponent(latS, true) &&
            isValidCoordinateComponent(lonM, true) && isValidCoordinateComponent(lonS, true)
          latDecimal = convertDMSToDecimal(math.abs(latD), latM, latS)
          lonDecimal = convertDMSToDecimal(math.abs(lonD), lonM, lonS)
          latSigned = if (latHem.toUpperCase == "S") -latDecimal else latDecimal
          lonSigned = if (lonHem.toUpperCase == "W") -lonDecimal else lonDecimal
          if isValidCoordinate(latSigned, lonSigned)
        } yield new GeoCoordinate(latSigned, lonSigned, belongsToArticle)

      // Case 4: Fractional parts in strings containing "/"
      case latStr :: lonStr :: _ if latStr.contains("/") && lonStr.contains("/") =>
        val latParts = latStr.split("/")
        val lonParts = lonStr.split("/")
        if (latParts.length == 3 && lonParts.length == 3) {
          val Array(latDeg, latMin, latHem) = latParts
          val Array(lonDeg, lonMin, lonHem) = lonParts
          for {
            latD <- toDoubleSafe(latDeg)
            latM <- toDoubleSafe(latMin)
            lonD <- toDoubleSafe(lonDeg)
            lonM <- toDoubleSafe(lonMin)
            if isValidCoordinateComponent(latM, true) && isValidCoordinateComponent(lonM, true)
            latDecimal = convertDMSToDecimal(math.abs(latD), latM, 0.0)
            lonDecimal = convertDMSToDecimal(math.abs(lonD), lonM, 0.0)
            latSigned = if (latHem.toUpperCase == "S") -latDecimal else latDecimal
            lonSigned = if (lonHem.toUpperCase == "W") -lonDecimal else lonDecimal
            if isValidCoordinate(latSigned, lonSigned)
          } yield new GeoCoordinate(latSigned, lonSigned, belongsToArticle)
        } else None

      // Case 5: {{coord|latitude|longitude}} - Latitude and longitude as decimal degrees (exactly 2 parameters)
      case latitude :: longitude :: Nil =>
        val latOpt = singleCoordParser.parseSingleCoordinate(latitude).flatMap(s => toDoubleSafe(s.toString)).orElse(toDoubleSafe(latitude))
        val lonOpt = singleCoordParser.parseSingleCoordinate(longitude).flatMap(s => toDoubleSafe(s.toString)).orElse(toDoubleSafe(longitude))
        (latOpt, lonOpt) match {
          case (Some(lat), Some(lon)) if isValidCoordinate(lat, lon) => Some(new GeoCoordinate(lat, lon, belongsToArticle))
          case _ => None
        }

      // Default fallback
      case _ => None
    }
  }

  private def parseGeoCoordinate(coordStr: String): Option[GeoCoordinate] = {
    coordStr match {
      case Coordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir) =>
        val latSeconds = Option(latSec).filterNot(_.isEmpty).map(_.toDouble).getOrElse(0.0)
        val lonSeconds = Option(lonSec).filterNot(_.isEmpty).map(_.toDouble).getOrElse(0.0)

        // Validate minutes and seconds are non-negative and less than 60
        if (!isValidCoordinateComponent(latMin.toDouble, true) ||
          !isValidCoordinateComponent(lonMin.toDouble, true) ||
          !isValidCoordinateComponent(latSeconds, true) ||
          !isValidCoordinateComponent(lonSeconds, true)) {
          return None
        }

        val latDecimal = convertDMSToDecimal(latDeg.toDouble, latMin.toDouble, latSeconds)
        val lonDecimal = convertDMSToDecimal(lonDeg.toDouble, lonMin.toDouble, lonSeconds)

        val latSigned = if (latDir.toUpperCase == "S") -latDecimal else latDecimal
        val lonSigned = if (lonDir.toUpperCase == "W") -lonDecimal else lonDecimal

        if (isValidCoordinate(latSigned, lonSigned)) {
          Some(new GeoCoordinate(latSigned, lonSigned, false))
        } else {
          None
        }
      case _ => None
    }
  }
}