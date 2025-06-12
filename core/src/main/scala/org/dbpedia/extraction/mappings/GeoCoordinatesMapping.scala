package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.{PropertyNode, TemplateNode}
import org.dbpedia.extraction.dataparser._
import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.util.matching.Regex
import scala.util.Try

/**
 * Simple GeoCoordinate class to hold coordinate data
 */
case class GeoCoordinate(latitude: Double, longitude: Double) {
  require(latitude >= -90.0 && latitude <= 90.0, s"Invalid latitude: $latitude")
  require(longitude >= -180.0 && longitude <= 180.0, s"Invalid longitude: $longitude")

  def this(latDeg: Double, latMin: Double, latSec: Double, latDir: String,
           lonDeg: Double, lonMin: Double, lonSec: Double, lonDir: String,
           dummy: Boolean) = {
    this(
      GeoCoordinate.dmsToDecimal(latDeg, latMin, latSec, latDir),
      GeoCoordinate.dmsToDecimal(lonDeg, lonMin, lonSec, lonDir)
    )
  }
}

object GeoCoordinate {
  def dmsToDecimal(degrees: Double, minutes: Double, seconds: Double, direction: String): Double = {
    val decimal = degrees + minutes / 60.0 + seconds / 3600.0
    if (direction == "S" || direction == "W") -decimal else decimal
  }
}



/**
 * Enhanced GeoCoordinatesMapping that supports all 4 Wikipedia coordinate template cases:
 * 1. Single coordinate string (e.g., "40.7128, -74.0060")
 * 2. Separate latitude and longitude decimal values
 * 3. Basic DMS (Degrees, Minutes, Seconds) components
 * 4. Full DMS with all components and directions
 */
class GeoCoordinatesMapping(
                             val ontologyProperty : OntologyProperty,
                             val coordinates : String,
                             val latitude : String,
                             val longitude : String,
                             val longitudeDegrees : String,
                             val longitudeMinutes : String,
                             val longitudeSeconds : String,
                             val longitudeDirection : String,
                             val latitudeDegrees : String,
                             val latitudeMinutes : String,
                             val latitudeSeconds : String,
                             val latitudeDirection : String,
                             context : {
                               def ontology : Ontology
                               def redirects : Redirects
                               def language : Language
                             }
                           ) extends PropertyMapping {

  private val logger = Logger.getLogger(classOf[GeoCoordinatesMapping].getName)

  private val doubleParser = new DoubleParser(context)
  private val doubleParserEn = new DoubleParser(context = new { def language : Language = Language("en") })
  private val wikiCode = context.language.wikiCode

  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val latOntProperty = context.ontology.properties("geo:lat")
  private val lonOntProperty = context.ontology.properties("geo:long")
  private val pointOntProperty = context.ontology.properties("georss:point")
  private val featureOntClass = context.ontology.classes("geo:SpatialThing")

  // Enhanced hemisphere properties
  private val latnsOntProperty = context.ontology.properties("dbp:latns")
  private val longewOntProperty = context.ontology.properties("dbp:longew")

  // Additional coordinate metadata properties
  private val coordPrecisionProperty = context.ontology.properties("dbp:coordPrecision")
  private val coordSourceProperty = context.ontology.properties("dbp:coordSource")
  private val coordFormatProperty = context.ontology.properties("dbp:coordFormat")

  override val datasets = Set(DBpediaDatasets.OntologyPropertiesGeo)

  override def extract(node: TemplateNode, subjectUri: String): Seq[Quad] = {
    extractGeoCoordinate(node) match {
      case Some((coord, metadata)) => writeGeoCoordinate(node, coord, metadata, subjectUri, node.sourceIri)
      case None => Seq.empty
    }
  }

  private def extractGeoCoordinate(node: TemplateNode): Option[(GeoCoordinate, CoordinateMetadata)] = {
    // Case 1: Single coordinate string (e.g., "40.7128, -74.0060")
    if (coordinates != null) {
      node.property(coordinates) match {
        case Some(coordProperty) =>
          parseCoordinateString(coordProperty) match {
            case Some(result) =>
              logger.log(Level.FINE, s"Extracted coordinates from single string: ${result._1}")
              return Some(result)
            case None => // Continue
          }
        case None => // Continue
      }
    }

    // Case 2: Separate latitude and longitude decimal values
    if (latitude != null && longitude != null) {
      (node.property(latitude), node.property(longitude)) match {
        case (Some(latitudeProperty), Some(longitudeProperty)) =>
          (getSingleCoordinate(latitudeProperty, -90.0, 90.0, wikiCode),
            getSingleCoordinate(longitudeProperty, -180.0, 180.0, wikiCode)) match {
            case (Some(lat), Some(lon)) =>
              try {
                val coord = new GeoCoordinate(lat, lon)
                val metadata = CoordinateMetadata("decimal", "separate_params", determinePrecision(lat, lon))
                logger.log(Level.FINE, s"Extracted coordinates from separate lat/lon: $coord")
                return Some((coord, metadata))
              } catch {
                case ex: IllegalArgumentException =>
                  logger.log(Level.FINE, "Invalid geo coordinate from separate lat/lon", ex)
              }
            case _ =>
          }
        case _ =>
      }
    }

    // Case 3 & 4: DMS (Degrees, Minutes, Seconds) format - both basic and full
    extractDMSCoordinates(node) match {
      case Some(result) =>
        logger.log(Level.FINE, s"Extracted coordinates from DMS: ${result._1}")
        return Some(result)
      case None => // Continue to alternative parameter extraction
    }

    // Fallback: Try alternative parameter names
    extractAlternativeParameters(node) match {
      case Some(result) =>
        logger.log(Level.FINE, s"Extracted coordinates from alternative parameters: ${result._1}")
        return Some(result)
      case None => None
    }
  }

  private def parseCoordinateString(coordProperty: PropertyNode): Option[(GeoCoordinate, CoordinateMetadata)] = {
    val coordStr = coordProperty.children.map(_.toPlainText).mkString.trim

    // Try different coordinate patterns
    for (pattern <- GeoCoordinateParserConfig.coordinatePatterns) {
      pattern.findFirstMatchIn(coordStr) match {
        case Some(m) =>
          parsePatternMatch(m, pattern) match {
            case Some(result) => return Some(result)
            case None => // Continue with next pattern
          }
        case None => // Continue with next pattern
      }
    }

    None
  }

  private def parsePatternMatch(m: Regex.Match, pattern: Regex): Option[(GeoCoordinate, CoordinateMetadata)] = {
    try {
      pattern.pattern.pattern match {
        // Decimal degrees: 40.7128, -74.0060
        case p if p.contains("(-?\\d+\\.?\\d*).*(-?\\d+\\.?\\d*)") =>
          val lat = m.group(1).toDouble
          val lon = m.group(2).toDouble
          val coord = new GeoCoordinate(lat, lon)
          val metadata = CoordinateMetadata("decimal", "pattern_match", determinePrecision(lat, lon))
          Some((coord, metadata))

        // DMS format: 40°42'46"N 74°00'22"W
        case p if p.contains("°.*'.*\".*[NS].*°.*'.*\".*[EW]") =>
          val latDeg = m.group(1).toDouble
          val latMin = m.group(2).toDouble
          val latSec = m.group(3).toDouble
          val latDir = m.group(4)
          val lonDeg = m.group(5).toDouble
          val lonMin = m.group(6).toDouble
          val lonSec = m.group(7).toDouble
          val lonDir = m.group(8)

          val coord = new GeoCoordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir, false)
          val metadata = CoordinateMetadata("dms", "pattern_match", "seconds")
          Some((coord, metadata))

        // DM format: 40°42.77'N 74°00.37'W
        case p if p.contains("°.*'.*[NS].*°.*'.*[EW]") =>
          val latDeg = m.group(1).toDouble
          val latMin = m.group(2).toDouble
          val latDir = m.group(3)
          val lonDeg = m.group(4).toDouble
          val lonMin = m.group(5).toDouble
          val lonDir = m.group(6)

          val coord = new GeoCoordinate(latDeg, latMin, 0.0, latDir, lonDeg, lonMin, 0.0, lonDir, false)
          val metadata = CoordinateMetadata("dm", "pattern_match", "minutes")
          Some((coord, metadata))

        // D format: 40.7128°N 74.0060°W
        case p if p.contains("°.*[NS].*°.*[EW]") =>
          val latDeg = m.group(1).toDouble
          val latDir = m.group(2)
          val lonDeg = m.group(3).toDouble
          val lonDir = m.group(4)

          val coord = new GeoCoordinate(latDeg, 0.0, 0.0, latDir, lonDeg, 0.0, 0.0, lonDir, false)
          val metadata = CoordinateMetadata("d", "pattern_match", "degrees")
          Some((coord, metadata))

        case _ => None
      }
    } catch {
      case ex: Exception =>
        logger.log(Level.FINE, s"Error parsing coordinate pattern: ${ex.getMessage}")
        None
    }
  }

  private def extractDMSCoordinates(node: TemplateNode): Option[(GeoCoordinate, CoordinateMetadata)] = {
    // Case 3: Basic DMS with degrees required, minutes/seconds optional
    if (longitudeDegrees != null && latitudeDegrees != null) {
      (node.property(latitudeDegrees), node.property(longitudeDegrees)) match {
        case (Some(latDegProperty), Some(lonDegProperty)) =>
          (doubleParser.parse(latDegProperty), doubleParser.parse(lonDegProperty)) match {
            case (Some(latDeg), Some(lonDeg)) =>
              val latMin = node.property(latitudeMinutes).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val latSec = node.property(latitudeSeconds).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val latDir = node.property(latitudeDirection).map(_.children.map(_.toPlainText).mkString.trim).getOrElse("N")

              val lonMin = node.property(longitudeMinutes).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val lonSec = node.property(longitudeSeconds).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val lonDir = node.property(longitudeDirection).map(_.children.map(_.toPlainText).mkString.trim).getOrElse("E")

              // Normalize direction indicators based on language
              val normalizedLatDir = normalizeLatitudeDirection(latDir, wikiCode)
              val normalizedLonDir = normalizeLongitudeDirection(lonDir, wikiCode)

              try {
                val coord = new GeoCoordinate(latDeg.value, latMin, latSec, normalizedLatDir, lonDeg.value, lonMin, lonSec, normalizedLonDir, false)
                val precision = if (latSec != 0.0 || lonSec != 0.0) "seconds"
                else if (latMin != 0.0 || lonMin != 0.0) "minutes"
                else "degrees"
                val metadata = CoordinateMetadata("dms", "structured_params", precision)
                return Some((coord, metadata))
              } catch {
                case ex: IllegalArgumentException =>
                  logger.log(Level.FINE, "Invalid DMS geo coordinate", ex)
              }
            case _ => // Continue
          }
        case _ => // Continue
      }
    }

    None
  }

  private def extractAlternativeParameters(node: TemplateNode): Option[(GeoCoordinate, CoordinateMetadata)] = {
    // Try alternative parameter names from the configuration
    val paramNames = GeoCoordinateParserConfig.coordinateParameterNames

    // Try single coordinate parameters
    for (coordParam <- paramNames.getOrElse("coordinates", List.empty)) {
      node.property(coordParam) match {
        case Some(prop) =>
          parseCoordinateString(prop) match {
            case Some(result) => return Some(result)
            case None => // Continue
          }
        case None => // Continue
      }
    }

    // Try separate lat/lon parameters
    for (latParam <- paramNames.getOrElse("latitude", List.empty);
         lonParam <- paramNames.getOrElse("longitude", List.empty)) {
      (node.property(latParam), node.property(lonParam)) match {
        case (Some(latProp), Some(lonProp)) =>
          (getSingleCoordinate(latProp, -90.0, 90.0, wikiCode),
            getSingleCoordinate(lonProp, -180.0, 180.0, wikiCode)) match {
            case (Some(lat), Some(lon)) =>
              try {
                val coord = new GeoCoordinate(lat, lon)
                val metadata = CoordinateMetadata("decimal", "alternative_params", determinePrecision(lat, lon))
                return Some((coord, metadata))
              } catch {
                case ex: IllegalArgumentException =>
                  logger.log(Level.FINE, "Invalid coordinate from alternative parameters", ex)
              }
            case _ => // Continue
          }
        case _ => // Continue
      }
    }

    // Try DMS alternative parameters
    for (latDegParam <- paramNames.getOrElse("lat_degrees", List.empty);
         lonDegParam <- paramNames.getOrElse("lon_degrees", List.empty)) {
      (node.property(latDegParam), node.property(lonDegParam)) match {
        case (Some(latDegProp), Some(lonDegProp)) =>
          (doubleParser.parse(latDegProp), doubleParser.parse(lonDegProp)) match {
            case (Some(latDeg), Some(lonDeg)) =>
              val latMin = paramNames.getOrElse("lat_minutes", List.empty).headOption
                .flatMap(node.property).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val latSec = paramNames.getOrElse("lat_seconds", List.empty).headOption
                .flatMap(node.property).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val latDir = paramNames.getOrElse("lat_direction", List.empty).headOption
                .flatMap(node.property).map(_.children.map(_.toPlainText).mkString.trim).getOrElse("N")

              val lonMin = paramNames.getOrElse("lon_minutes", List.empty).headOption
                .flatMap(node.property).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val lonSec = paramNames.getOrElse("lon_seconds", List.empty).headOption
                .flatMap(node.property).flatMap(doubleParser.parse).map(_.value).getOrElse(0.0)
              val lonDir = paramNames.getOrElse("lon_direction", List.empty).headOption
                .flatMap(node.property).map(_.children.map(_.toPlainText).mkString.trim).getOrElse("E")

              val normalizedLatDir = normalizeLatitudeDirection(latDir, wikiCode)
              val normalizedLonDir = normalizeLongitudeDirection(lonDir, wikiCode)

              try {
                val coord = new GeoCoordinate(latDeg.value, latMin, latSec, normalizedLatDir, lonDeg.value, lonMin, lonSec, normalizedLonDir, false)
                val precision = if (latSec != 0.0 || lonSec != 0.0) "seconds"
                else if (latMin != 0.0 || lonMin != 0.0) "minutes"
                else "degrees"
                val metadata = CoordinateMetadata("dms", "alternative_dms_params", precision)
                return Some((coord, metadata))
              } catch {
                case ex: IllegalArgumentException =>
                  logger.log(Level.FINE, "Invalid DMS coordinate from alternative parameters", ex)
              }
            case _ => // Continue
          }
        case _ => // Continue
      }
    }

    None
  }

  private def normalizeLatitudeDirection(direction: String, langCode: String): String = {
    val dirMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(langCode,
      GeoCoordinateParserConfig.latitudeLetterMap.getOrElse("en", Map.empty[String, String]))

    val cleanDir = direction.trim.toUpperCase
    dirMap.getOrElse(cleanDir,
      dirMap.getOrElse(cleanDir.take(1), "N")) // Fallback to first character or "N"
  }

  private def normalizeLongitudeDirection(direction: String, langCode: String): String = {
    val dirMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(langCode,
      GeoCoordinateParserConfig.longitudeLetterMap.getOrElse("en", Map.empty[String, String]))

    val cleanDir = direction.trim.toUpperCase
    dirMap.getOrElse(cleanDir,
      dirMap.getOrElse(cleanDir.take(1), "E")) // Fallback to first character or "E"
  }

  private def determinePrecision(lat: Double, lon: Double): String = {
    val latStr = lat.toString
    val lonStr = lon.toString
    val latDecimals = if (latStr.contains(".")) latStr.split("\\.")(1).length else 0
    val lonDecimals = if (lonStr.contains(".")) lonStr.split("\\.")(1).length else 0
    val maxDecimals = Math.max(latDecimals, lonDecimals)

    if (maxDecimals >= 5) "high" // ~1 meter precision
    else if (maxDecimals >= 3) "medium" // ~100 meter precision
    else "low" // ~10km precision
  }

  private def determinePrecision(coord: GeoCoordinate): String = {
    determinePrecision(coord.latitude, coord.longitude)
  }

  private def writeGeoCoordinate(node: TemplateNode, coord: GeoCoordinate, metadata: CoordinateMetadata, subjectUri: String, sourceUri: String): Seq[Quad] = {
    var quads = new ArrayBuffer[Quad]()
    var instanceUri = subjectUri

    if (ontologyProperty != null) {
      instanceUri = node.generateUri(subjectUri, ontologyProperty.name)
      quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, subjectUri, ontologyProperty, instanceUri, sourceUri)
    }

    // Core coordinate properties
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, typeOntProperty, featureOntClass.uri, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, latOntProperty, coord.latitude.toString, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, lonOntProperty, coord.longitude.toString, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri)

    // Enhanced hemisphere indicators
    val latHemisphere = if (coord.latitude < 0) "S" else "N"
    val lonHemisphere = if (coord.longitude < 0) "W" else "E"

    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, latnsOntProperty, latHemisphere, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, longewOntProperty, lonHemisphere, sourceUri)

    // Additional metadata properties
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, coordFormatProperty, metadata.format, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, coordSourceProperty, metadata.source, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.OntologyPropertiesGeo, instanceUri, coordPrecisionProperty, metadata.precision, sourceUri)

    quads
  }

  private def getSingleCoordinate(coordinateProperty: PropertyNode, rangeMin: Double, rangeMax: Double, wikiCode: String): Option[Double] = {
    // Try to parse as double directly from the property text
    val coordText = coordinateProperty.children.map(_.toPlainText).mkString.trim

    Try(coordText.toDouble).toOption match {
      case Some(coordinateValue) =>
        if (rangeMin <= coordinateValue && coordinateValue <= rangeMax) {
          Some(coordinateValue)
        } else None
      case None =>
        // Try with double parser
        doubleParser.parse(coordinateProperty) match {
          case Some(parseResult) =>
            if (rangeMin <= parseResult.value && parseResult.value <= rangeMax) {
              Some(parseResult.value)
            } else if (wikiCode != "en") {
              doubleParserEn.parse(coordinateProperty) match {
                case Some(enCoordinateValue) =>
                  if (rangeMin <= enCoordinateValue.value && enCoordinateValue.value <= rangeMax) {
                    Some(enCoordinateValue.value)
                  } else None
                case None => None
              }
            } else None
          case None => None
        }
    }
  }
}

/**
 * Case class to hold coordinate metadata information
 */
case class CoordinateMetadata(
                               format: String,    // "decimal", "dms", "dm", "d", "mixed"
                               source: String,    // "single_string", "separate_params", "structured_params", etc.
                               precision: String  // "degrees", "minutes", "seconds", "low", "medium", "high"
                             )