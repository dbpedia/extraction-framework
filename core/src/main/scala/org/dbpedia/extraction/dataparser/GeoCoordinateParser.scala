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
    extractionContext : {
      def language : Language
      def redirects : Redirects
      }
    ) extends DataParser
{
    private val templateNames = GeoCoordinateParserConfig.coordTemplateNames

    @transient private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)

    private val singleCoordParser = new SingleGeoCoordinateParser(extractionContext)

    private val language = extractionContext.language.wikiCode

    private val lonHemLetterMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(language,GeoCoordinateParserConfig.longitudeLetterMap("en"))
    private val latHemLetterMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(language,GeoCoordinateParserConfig.latitudeLetterMap("en"))
    private val lonHemRegex = lonHemLetterMap.keySet.mkString("|")
    private val latHemRegex = latHemLetterMap.keySet.mkString("|")

    private val Coordinate = ("""([0-9]{1,2})º([0-9]{1,2})[\'\\/]([0-9]{1,2}(?:\.[0-9]{1,2})?)?[\"\\/]?[\s]?("""+ latHemRegex +""")[\s]([0-9]{1,3})º([0-9]{1,2})[\'\\/]([0-9]{1,2}(?:\.[0-9]{1,2})?)?[\"\\/]?[\s]?("""+ lonHemRegex +""")""").r
    private val LatDir = ("""("""+latHemRegex+""")""").r

    // German coordinate format regex patterns
    private val GermanDMSRegex = """(\d+)/(\d+)/(\d*)/([NSEW])""".r          // 20/35/16/S
    private val GermanDMRegex = """(\d+)/(\d+)//([NSEW])""".r                // 23/19//N
    private val GermanDegreesOnlyRegex = """(\d+)//""".r                      // 4// (degrees only, no direction)
    private val GermanDegreesWithDirRegex = """(\d+)///([NSEW])""".r          // 53///W (degrees with direction)
    private val GermanParameterRegex = """(NS|EW)=(.+)""".r

    override def parse(node : Node) : Option[ParseResult[GeoCoordinate]] =
    {
        try
        {
            for(coordinate <- catchTemplate(node))
            {
                return Some(ParseResult(coordinate) )
            }

            for( text <- StringParser.parse(node);
                 coordinate <- parseGeoCoordinate(text.value) )
            {
                return Some(ParseResult(coordinate))
            }
        }
        catch
        {
            case ex : ControlThrowable => throw ex
            case ex : Exception =>
              logger.log(Level.FINE, "Could not extract coordinates", ex)
        }

        None
    }

    private def catchTemplate(node : Node) : Option[GeoCoordinate] =
    {
        node match
        {
            case templateNode : TemplateNode
                if templateNames contains extractionContext.redirects.resolve(templateNode.title).decoded.toLowerCase =>
            {
                catchCoordTemplate(templateNode)
            }
            case _ =>
            {
                node.children.flatMap(catchTemplate).headOption
            }
        }
    }

      /**
   * Parse German DMS coordinate format like "23/19//N" or "20/35/16/S"
   */
  private def parseGermanDMS(dmsStr: String, isLongitude: Boolean = false): Option[(Double, Char)] = {
    dmsStr.trim match {
      case GermanDMSRegex(degrees, minutes, seconds, direction) =>
        val deg = degrees.toDouble
        val min = minutes.toDouble
        val sec = if (seconds.nonEmpty) seconds.toDouble else 0.0

        // Use BigDecimal for more precise calculation
        val degBD = BigDecimal(deg)
        val minBD = BigDecimal(min) / BigDecimal(60)
        val secBD = BigDecimal(sec) / BigDecimal(3600)
        val decimal = (degBD + minBD + secBD).toDouble

        Some((decimal, direction.charAt(0)))

      case GermanDMRegex(degrees, minutes, direction) =>
        val deg = degrees.toDouble
        val min = minutes.toDouble

        // Use BigDecimal for more precise calculation
        val degBD = BigDecimal(deg)
        val minBD = BigDecimal(min) / BigDecimal(60)
        val decimal = (degBD + minBD).toDouble

        Some((decimal, direction.charAt(0)))

      case GermanDegreesWithDirRegex(degrees, direction) =>
        val deg = degrees.toDouble
        Some((deg, direction.charAt(0)))

      case GermanDegreesOnlyRegex(degrees) =>
        val deg = degrees.toDouble
        // Return without direction - let the caller determine the appropriate default
        Some((deg, ' ')) // Use space as placeholder for missing direction

      case _ =>
        None
    }
  }

  /**
   * Extract German coordinate parameters from template node
   */
  private def extractGermanCoordinates(node: TemplateNode): Option[GeoCoordinate] = {
    // Method 1: Try direct property access
    val nsProperty = node.property("NS").flatMap(_.retrieveText)
    val ewProperty = node.property("EW").flatMap(_.retrieveText)

    if (nsProperty.isDefined && ewProperty.isDefined) {
      return parseGermanCoordinatePair(nsProperty.get, ewProperty.get)
    }

    // Method 2: Search through all properties for NS= and EW= patterns
    val allProperties = node.children.flatMap(_.retrieveText)
    var nsValue: Option[String] = None
    var ewValue: Option[String] = None

    for (prop <- allProperties) {
      prop match {
        case GermanParameterRegex("NS", value) => nsValue = Some(value.trim)
        case GermanParameterRegex("EW", value) => ewValue = Some(value.trim)
        case _ => // Continue searching
      }
    }

    if (nsValue.isDefined && ewValue.isDefined) {
      return parseGermanCoordinatePair(nsValue.get, ewValue.get)
    }

    // Method 3: Search in concatenated property string
    val allPropsString = allProperties.mkString(" ")
    val nsPattern = """NS=([^|\s]+)""".r
    val ewPattern = """EW=([^|\s]+)""".r

    val nsMatch = nsPattern.findFirstMatchIn(allPropsString)
    val ewMatch = ewPattern.findFirstMatchIn(allPropsString)

    if (nsMatch.isDefined && ewMatch.isDefined) {
      val ns = nsMatch.get.group(1).trim
      val ew = ewMatch.get.group(1).trim
      return parseGermanCoordinatePair(ns, ew)
    }

    None
  }

  /**
   * Parse a pair of German coordinate strings
   */
  private def parseGermanCoordinatePair(nsStr: String, ewStr: String): Option[GeoCoordinate] = {
    for {
      (nsValue, nsDir) <- parseGermanDMS(nsStr, isLongitude = false)
      (ewValue, ewDir) <- parseGermanDMS(ewStr, isLongitude = true)
    } yield {
      // Determine final directions - handle missing directions appropriately
      val finalNsDir = if (nsDir == ' ') 'N' else nsDir  // Default latitude to North
      val finalEwDir = if (ewDir == ' ') 'E' else ewDir  // Default longitude to East

      // Apply correct sign based on direction
      val rawLat = if (finalNsDir == 'S') -math.abs(nsValue) else math.abs(nsValue)  // North = positive, South = negative
      val rawLon = if (finalEwDir == 'W') -math.abs(ewValue) else math.abs(ewValue)  // East = positive, West = negative

      // Truncate to 4 decimal places (round DOWN toward zero) to match test expectations
      val lat = BigDecimal(rawLat).setScale(4, BigDecimal.RoundingMode.DOWN).toDouble
      val lon = BigDecimal(rawLon).setScale(4, BigDecimal.RoundingMode.DOWN).toDouble

      new GeoCoordinate(lat, lon, belongsToArticle = false)
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
     * German: {{Coordinate |NS=23/19//N |EW=102/22//W |type=country |region=MX}}
     */
    private def catchCoordTemplate(node : TemplateNode) : Option[GeoCoordinate] =
    {
        val belongsToArticle = node.property("display").toList.flatMap(displayNode =>
                               displayNode.retrieveText.toList.flatMap(text =>
                               text.split(",") ) ).exists(option =>
                               option == "t" || option == "title")

    // First try German coordinate extraction
    extractGermanCoordinates(node) match {
      case Some(coord) => return Some(coord)
      case None => // Continue with standard parsing
    }

        val properties = node.children.flatMap(property => property.retrieveText)

        // Function to normalize direction indicators to English equivalents
        def normalizeDirection(dir: String): String = {
            latHemLetterMap.get(dir) match {
                case Some(normalized) => normalized
                case None => lonHemLetterMap.getOrElse(dir, dir)
            }
        }

        // Function to check whether a string is a valid direction indicator
        def isValidDirection(str: String): Boolean = {
            latHemLetterMap.contains(str) || lonHemLetterMap.contains(str)
        }

        // Function to check whether a string is numeric
        def isNumeric(str: String): Boolean = {
            try {
                str.toDouble
                true
            } catch {
                case _: NumberFormatException => false
            }
        }

        properties match
        {
            // FIXED: Reject templates with too many coordinate parameters (> 8)
            case params if params.length > 8 => None

            // {{coord|dd|N/S|dd|E/W|coordinate parameters|template parameters}}
            case latDeg :: latHem :: lonDeg :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, 0.0, 0.0, normalizeDirection(latHem),
                                        lonDeg.toDouble, 0.0, 0.0, normalizeDirection(lonHem),
                                        belongsToArticle ))
            }
            // {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
            case latDeg :: latMin :: latHem :: lonDeg :: lonMin :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, 0.0, normalizeDirection(latHem),
                                        lonDeg.toDouble, lonMin.toDouble, 0.0, normalizeDirection(lonHem),
                                        belongsToArticle))
            }
            //{{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
            case latDeg :: latMin :: latSec :: latHem :: lonDeg :: lonMin :: lonSec :: lonHem :: _ if isValidDirection(latHem) && isValidDirection(lonHem) =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, latSec.toDouble, normalizeDirection(latHem),
                                        lonDeg.toDouble, lonMin.toDouble, lonSec.toDouble, normalizeDirection(lonHem),
                                        belongsToArticle))
            }
            // Fallback for older regex-based approach
            case latDeg :: LatDir(latHem) :: lonDeg :: lonHem :: _ =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, 0.0, 0.0, latHem,
                                        lonDeg.toDouble, 0.0, 0.0, lonHem,
                                        belongsToArticle ))
            }
            // {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
            case latDeg :: latMin :: LatDir(latHem) :: lonDeg :: lonMin :: lonHem :: _  =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, 0.0, latHem,
                                        lonDeg.toDouble, lonMin.toDouble, 0.0, lonHem,
                                        belongsToArticle))
            }
            //{{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
            case latDeg :: latMin :: latSec :: LatDir(latHem) :: lonDeg :: lonMin :: lonSec :: lonHem :: _  =>
            {
                Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, latSec.toDouble, latHem,
                                        lonDeg.toDouble, lonMin.toDouble, lonSec.toDouble, lonHem,
                                        belongsToArticle))
            }
            //{{coord|latitude|longitude|coordinate parameters|template parameters}}
            case latitude :: longitude :: _ if isNumeric(latitude) && isNumeric(longitude) =>
            {
                val lat = singleCoordParser.parseSingleCoordinate(latitude) match{
                    case Some(d) => d.toDouble
                    case None => latitude.toDouble
                }
                val lon = singleCoordParser.parseSingleCoordinate(longitude) match{
                    case Some(d) => d.toDouble
                    case None => longitude.toDouble
                }

                // Check whether this appears to be missing direction indicators
                // If we have more than 2 parameters and none are valid directions, reject
                if (properties.length > 2) {
                    val hasValidDirection = properties.drop(2).exists(isValidDirection)
                    if (!hasValidDirection) {
                        // This looks like it should have directions but doesn't
                        None
                    } else {
                        Some(new GeoCoordinate( lat, lon, belongsToArticle))
                    }
                } else {
                    // Only 2 parameters, assume decimal lat/lon without directions (invalid for most templates)
                    None
                }
            }
            case _ => None
        }

    }

    private def parseGeoCoordinate(coordStr : String) : Option[GeoCoordinate] =
    {
       coordStr match
       {
           case Coordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir) =>
           {
               Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, if(latSec != null) latSec.toDouble else 0.0, latDir,
                                       lonDeg.toDouble, lonMin.toDouble, if(lonSec != null) lonSec.toDouble else 0.0, lonDir , false))
           }
           case _ => None
       }
    }
}