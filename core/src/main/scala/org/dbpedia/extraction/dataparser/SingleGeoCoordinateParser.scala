package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{TemplateNode, Node}
import java.util.logging.{Level, Logger}
import util.control.ControlThrowable
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig
import org.dbpedia.extraction.mappings.Redirects

/**
 * Parses a single geographical coordinate, ie. either a latitude or a longitude.
 */
class SingleGeoCoordinateParser() extends DataParser
{
    private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)

    override def parse(node : Node) : Option[SingleGeoCoordinate] =
    {
        try
        {
            for( text <- StringParser.parse(node);
                 coordinate <- parseSingleCoordinate(text) )
            {
                return Some(coordinate)
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
    	import SingleGeoCoordinateParser.LatitudeRegex
    	import SingleGeoCoordinateParser.LongitudeRegex
    	
    	coordStr match {
    	  case LatitudeRegex(latDeg, latMin, latSec, latHem) => Some(new Latitude(latDeg.toDouble, latMin.toDouble, ("0"+latSec).toDouble, latHem))
    	  case LongitudeRegex(lonDeg, lonMin, lonSec, lonHem) => Some(new Longitude(lonDeg.toDouble, lonMin.toDouble, ("0"+lonSec).toDouble, lonHem))
    	  case _ => None
    	}
    }
        
}

object SingleGeoCoordinateParser
{
    private val LatitudeRegex = """([0-9]{1,2})/([0-9]{1,2})/([0-9]{0,2}(?:.[0-9]{1,2})?)[/]?[\s]?(N|S)""".r
    private val LongitudeRegex = """([0-9]{1,2})/([0-9]{1,2})/([0-9]{0,2}(?:.[0-9]{1,2})?)[/]?[\s]?(E|W|O)""".r

}