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
  private val templateNames = GeoCoordinateParserConfig.coordTemplateNames

  @transient private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)

  private val singleCoordParser = new SingleGeoCoordinateParser(extractionContext)

  private val language = extractionContext.language.wikiCode

  private val lonHemLetterMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.longitudeLetterMap("en"))
  private val latHemLetterMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(language, GeoCoordinateParserConfig.latitudeLetterMap("en"))
  private val lonHemRegex = lonHemLetterMap.keySet.mkString("|")
  private val latHemRegex = latHemLetterMap.keySet.mkString("|")

  private val Coordinate = ("""([0-9]{1,2})º([0-9]{1,2})[\'\\/]([0-9]{1,2}(?:\.[0-9]{1,2})?)?[\"\\/]?[\s]?(""" + latHemRegex + """)[\s]([0-9]{1,3})º([0-9]{1,2})[\'\\/]([0-9]{1,2}(?:\.[0-9]{1,2})?)?[\"\\/]?[\s]?(""" + lonHemRegex + """)""").r
  private val LatDir = ("""(""" + latHemRegex + """)""").r
  private val LonDir = ("""(""" + lonHemRegex + """)""").r

  override def parse(node: Node): Option[ParseResult[GeoCoordinate]] = {
    try {
      for (coordinate <- catchTemplate(node)) {
        return Some(ParseResult(coordinate))
      }

      for (text <- StringParser.parse(node);
           coordinate <- parseGeoCoordinate(text.value)) {
        return Some(ParseResult(coordinate))
      }
    }
    catch {
      case ex: ControlThrowable => throw ex
      case ex: Exception =>
        logger.log(Level.FINE, "Could not extract coordinates", ex)
    }

    None
  }

  private def catchTemplate(node: Node): Option[GeoCoordinate] = {
    node match {
      case templateNode: TemplateNode
        if templateNames contains extractionContext.redirects.resolve(templateNode.title).decoded.toLowerCase => {
        catchCoordTemplate(templateNode)
      }
      case _ => {
        node.children.flatMap(catchTemplate).headOption
      }
    }
  }

  /**
   * Catches the coord template
   *
   * Examples:
   * {{coord|latitude|longitude|coordinate parameters|template parameters}}
   * {{coord|dd|N/S|dd|E/W|coordinate parameters|template parameters}}
   * {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
   * {{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
   */
  private def catchCoordTemplate(node: TemplateNode): Option[GeoCoordinate] = {
    val belongsToArticle = node.property("display").toList.flatMap(displayNode =>
      displayNode.retrieveText.toList.flatMap(text =>
        text.split(","))).exists(option =>
      option == "t" || option == "title")

    val properties = node.children.flatMap(property => property.retrieveText)

    // Function to normalize direction indicators to English equivalents
    def normalizeDirection(dir: String): String = {
      latHemLetterMap.get(dir) match {
        case Some(normalized) => normalized
        case None => lonHemLetterMap.getOrElse(dir, dir)
      }
    }

    // Function to check if a string is a valid direction indicator
    def isValidDirection(str: String): Boolean = {
      latHemLetterMap.contains(str) || lonHemLetterMap.contains(str)
    }

    // Function to check if a string is numeric
    def isNumeric(str: String): Boolean = {
      try {
        str.toDouble
        true
      } catch {
        case _: NumberFormatException => false
      }
    }

    properties match {
      // FIXED: Reject templates with too many coordinate parameters (> 8)
      case params if params.length > 8 => None

      // {{coord|dd|N/S|dd|E/W|coordinate parameters|template parameters}}
      case latDeg :: latHem :: lonDeg :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) => {
        Some(new GeoCoordinate(latDeg.toDouble, 0.0, 0.0, normalizeDirection(latHem),
          lonDeg.toDouble, 0.0, 0.0, normalizeDirection(lonHem),
          belongsToArticle))
      }
      // {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
      case latDeg :: latMin :: latHem :: lonDeg :: lonMin :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) => {
        Some(new GeoCoordinate(latDeg.toDouble, latMin.toDouble, 0.0, normalizeDirection(latHem),
          lonDeg.toDouble, lonMin.toDouble, 0.0, normalizeDirection(lonHem),
          belongsToArticle))
      }
      //{{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
      case latDeg :: latMin :: latSec :: latHem :: lonDeg :: lonMin :: lonSec :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) => {
        Some(new GeoCoordinate(latDeg.toDouble, latMin.toDouble, latSec.toDouble, normalizeDirection(latHem),
          lonDeg.toDouble, lonMin.toDouble, lonSec.toDouble, normalizeDirection(lonHem),
          belongsToArticle))
      }
      // Fallback for older regex-based approach
      case latDeg :: LatDir(latHem) :: lonDeg :: lonHem :: _ => {
        Some(new GeoCoordinate(latDeg.toDouble, 0.0, 0.0, latHem,
          lonDeg.toDouble, 0.0, 0.0, lonHem,
          belongsToArticle))
      }
      // {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
      case latDeg :: latMin :: LatDir(latHem) :: lonDeg :: lonMin :: lonHem :: _ => {
        Some(new GeoCoordinate(latDeg.toDouble, latMin.toDouble, 0.0, latHem,
          lonDeg.toDouble, lonMin.toDouble, 0.0, lonHem,
          belongsToArticle))
      }
      //{{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
      case latDeg :: latMin :: latSec :: LatDir(latHem) :: lonDeg :: lonMin :: lonSec :: lonHem :: _ => {
        Some(new GeoCoordinate(latDeg.toDouble, latMin.toDouble, latSec.toDouble, latHem,
          lonDeg.toDouble, lonMin.toDouble, lonSec.toDouble, lonHem,
          belongsToArticle))
      }
      //{{coord|latitude|longitude|coordinate parameters|template parameters}}
      case latitude :: longitude :: _ if isNumeric(latitude) && isNumeric(longitude) => {
        // FIXED: Only parse as decimal coordinates if both are numeric and no direction indicators present
        // This prevents cases like "{{coord|40.7589|73.9851}}" without directions from being accepted
        val lat = singleCoordParser.parseSingleCoordinate(latitude) match {
          case Some(d) => d.toDouble
          case None => latitude.toDouble
        }
        val lon = singleCoordParser.parseSingleCoordinate(longitude) match {
          case Some(d) => d.toDouble
          case None => longitude.toDouble
        }

        // Check if this appears to be missing direction indicators
        // If we have more than 2 parameters and none are valid directions, reject
        if (properties.length > 2) {
          val hasValidDirection = properties.drop(2).exists(isValidDirection)
          if (!hasValidDirection) {
            // This looks like it should have directions but doesn't
            None
          } else {
            Some(new GeoCoordinate(lat, lon, belongsToArticle))
          }
        } else {
          // Only 2 parameters, assume decimal lat/lon without directions (invalid for most templates)
          None
        }
      }
      case _ => None
    }

  }

  private def parseGeoCoordinate(coordStr: String): Option[GeoCoordinate] = {
    coordStr match {
      case Coordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir) => {
        Some(new GeoCoordinate(latDeg.toDouble, latMin.toDouble, if (latSec != null) latSec.toDouble else 0.0, latDir,
          lonDeg.toDouble, lonMin.toDouble, if (lonSec != null) lonSec.toDouble else 0.0, lonDir, false))
      }
      case _ => None
    }
  }
}