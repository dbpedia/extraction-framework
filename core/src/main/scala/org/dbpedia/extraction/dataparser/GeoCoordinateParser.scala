package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{TemplateNode, Node}
import java.util.logging.{Level, Logger}
import util.control.ControlThrowable
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.config.dataparser.GeoCoordinateParserConfig

/**
 * Parses geographical coordinates.
 */
class GeoCoordinateParser(extractionContext : ExtractionContext) extends DataParser
{
    private val templateNames = GeoCoordinateParserConfig.coordTemplateNames

    private val logger = Logger.getLogger(classOf[GeoCoordinateParser].getName)

    override def parse(node : Node) : Option[GeoCoordinate] =
    {
        try
        {
            for(coordinate <- catchTemplate(node))
            {
                return Some(coordinate)        
            }

            for( text <- StringParser.parse(node);
                 coordinate <- parseGeoCoordinate(text) )
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
     * Catches the coord template
     *
     * Examples:
     * {{coord|latitude|longitude|coordinate parameters|template parameters}}
     * {{coord|dd|N/S|dd|E/W|coordinate parameters|template parameters}}
     * {{coord|dd|mm|N/S|dd|mm|E/W|coordinate parameters|template parameters}}
     * {{coord|dd|mm|ss|N/S|dd|mm|ss|E/W|coordinate parameters|template parameters}}
     */ 
    private def catchCoordTemplate(node : TemplateNode) : Option[GeoCoordinate] =
    {
        import GeoCoordinateParser.LatDir

        val belongsToArticle = node.property("display").toList.flatMap(displayNode =>
                               displayNode.retrieveText.toList.flatMap(text =>
                               text.split(",") ) ).exists(option =>
                               option == "t" || option == "title")

        val properties = node.children.flatMap(property => property.retrieveText)

        properties match
        {
            // {{coord|dd|N/S|dd|E/W|coordinate parameters|template parameters}}
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
            case latitude :: longitude :: _ =>
            {
                Some(new GeoCoordinate( latDeg = latitude.toDouble,
                                        lonDeg = longitude.toDouble,
                                        belongsToArticle = belongsToArticle))
            }
            case _ => None
        }

    }

    private def parseGeoCoordinate(coordStr : String) : Option[GeoCoordinate] =
    {
       import GeoCoordinateParser.Coordinate

       coordStr match
       {
           case Coordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir) =>
           {
               Some(new GeoCoordinate( latDeg.toDouble, latMin.toDouble, if(latSec != null) latSec.toDouble else 0.0, latDir,
                                       lonDeg.toDouble, lonMin.toDouble, if(lonSec != null) lonSec.toDouble else 0.0, lonDir ))
           }
           case _ => None
       }
    }
}

object GeoCoordinateParser
{
    private val Coordinate = """([0-9]{1,2})º([0-9]{1,2})\'([0-9]{1,2}(?:\.[0-9]{1,2})?)?\"?[\s]?(N|S)[\s]([0-9]{1,3})º([0-9]{1,2})\'([0-9]{1,2}(?:\.[0-9]{1,2})?)?\"?[\s]?(E|W|O)""".r

    private val LatDir = "(N|S)".r
}