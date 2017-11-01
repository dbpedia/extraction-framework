package org.dbpedia.extraction.dataparser

import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}

import util.control.ControlThrowable
import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig
import org.dbpedia.extraction.mappings.Redirects

import scala.language.reflectiveCalls

/**
 * Parses a single geographical coordinate, ie. either a latitude or a longitude.
 */
@SoftwareAgentAnnotation(classOf[SingleGeoCoordinateParser], AnnotationType.Parser)
class SingleGeoCoordinateParser(context : { def language : Language }) extends DataParser[SingleGeoCoordinate]
{
    private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)
    private val language = context.language.wikiCode
    
    private val lonHemLetterMap = GeoCoordinateParserConfig.longitudeLetterMap.getOrElse(language,GeoCoordinateParserConfig.longitudeLetterMap("en"))
    private val latHemLetterMap = GeoCoordinateParserConfig.latitudeLetterMap.getOrElse(language,GeoCoordinateParserConfig.latitudeLetterMap("en"))
    
	private val lonHemRegex = lonHemLetterMap.keySet.mkString("|")
	private val LongitudeRegex = ("""([0-9]{1,2})/([0-9]{1,2})/([0-9]{0,2}(?:.[0-9]{1,2})?)[/]?[\s]?("""+ lonHemRegex +""")""").r
	
	private val latHemRegex = latHemLetterMap.keySet.mkString("|")
	private val LatitudeRegex = ("""([0-9]{1,2})/([0-9]{1,2})/([0-9]{0,2}(?:.[0-9]{1,2})?)[/]?[\s]?("""+ latHemRegex +""")""").r


  private[dataparser]   override def parse(node : Node) : Option[ParseResult[SingleGeoCoordinate]] =
    {
        try
        {
            for( text <- StringParser.parse(node);
                 coordinate <- parseSingleCoordinate(text.value) )
            {
                return Some(ParseResult(coordinate))
            }
        }
        catch
        {
            case ex : ControlThrowable => throw ex
            case ex : Exception => logger.log(Level.FINE, "Could not extract coordinates", ex)
        }

        None
    }

    
    def parseSingleCoordinate(coordStr : String) : Option[SingleGeoCoordinate] = 
    {
    	coordStr match {
    	  case LatitudeRegex(latDeg, latMin, latSec, latHem) => Some(new Latitude(latDeg.toDouble, latMin.toDouble, ("0"+latSec).toDouble, latHemLetterMap.getOrElse(latHem,"N")))
    	  case LongitudeRegex(lonDeg, lonMin, lonSec, lonHem) => Some(new Longitude(lonDeg.toDouble, lonMin.toDouble, ("0"+lonSec).toDouble, lonHemLetterMap.getOrElse(lonHem,"E")))
    	  case _ => None
    	}
    }
        
}
