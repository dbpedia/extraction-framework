package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer

/**
 * Extracts geo-coodinates.
 */
class GeoCoordinatesMapping( ontologyProperty : OntologyProperty,
                             //TODO CreateMappingStats requires this properties to be public. Is there a better way?
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
                                 def redirects : Redirects   // redirects required by GeoCoordinatesParser
                                 def language : Language } ) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[GeoCoordinatesMapping].getName) 

    private val geoCoordinateParser = new GeoCoordinateParser(context)
    private val doubleParser = new DoubleParser(context)
    private val stringParser = StringParser

    private val typeOntProperty = context.ontology.properties("rdf:type")
    private val latOntProperty = context.ontology.properties("geo:lat")
    private val lonOntProperty = context.ontology.properties("geo:long")
    private val pointOntProperty = context.ontology.properties("georss:point")
    private val featureOntClass =  context.ontology.classes("gml:_Feature")

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        extractGeoCoordinate(node) match
        {
            case Some(coord) => writeGeoCoordinate(node, coord, subjectUri, node.sourceUri, pageContext)
            case None => Seq.empty
        }
    }

    private def extractGeoCoordinate(node : TemplateNode) : Option[GeoCoordinate] =
    {
        // case 1: coordinates set (all coordinates in one template property)
        if(coordinates != null)
        {
            for( coordProperty <- node.property(coordinates);
                 geoCoordinate <- geoCoordinateParser.parse(coordProperty) )
            {
                return Some(geoCoordinate)
            }
        }

        // case 2: latitude and longitude set (all coordinates in two template properties)
        if(latitude != null && longitude != null)
        {
            for( latitudeProperty <- node.property(latitude);
                 longitudeProperty <- node.property(longitude);
                 lat <- doubleParser.parse(latitudeProperty);
                 lon <- doubleParser.parse(longitudeProperty))
           {
               try
               {
                   return Some(new GeoCoordinate(latDeg = lat.toDouble, lonDeg = lon.toDouble))
               }
               catch
               {
                   case ex : IllegalArgumentException  => logger.log(Level.FINE, "Invalid geo coordinate", ex); return None
               }
           }
        }

        // case 3: more than two latitude and longitude properties (all coordinates in more than two template properties)
        if(longitudeDegrees != null && latitudeDegrees != null)
        {
            for( latDegProperty <- node.property(latitudeDegrees);
                 lonDegProperty <- node.property(longitudeDegrees);
                 latDeg <- doubleParser.parse(latDegProperty);
                 lonDeg <- doubleParser.parse(lonDegProperty) )
             {
                 val latMin = node.property(latitudeMinutes).flatMap(doubleParser.parse).getOrElse(0.0)
                 val latSec = node.property(latitudeSeconds).flatMap(doubleParser.parse).getOrElse(0.0)
                 val latDir = node.property(latitudeDirection).flatMap(stringParser.parse).getOrElse("N")

                 val lonMin = node.property(longitudeMinutes).flatMap(doubleParser.parse).getOrElse(0.0)
                 val lonSec = node.property(longitudeSeconds).flatMap(doubleParser.parse).getOrElse(0.0)
                 val lonDir = node.property(longitudeDirection).flatMap(stringParser.parse).getOrElse("E")

                 try
                 {
                     return Some(new GeoCoordinate(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir))
                 }
                 catch
                 {
                    case ex : IllegalArgumentException  => logger.log(Level.FINE, "Invalid geo coordinate", ex); return None
                 }
             }
        }

        None
    }

    private def writeGeoCoordinate(node : TemplateNode, coord : GeoCoordinate, subjectUri : String, sourceUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        var quads = new ArrayBuffer[Quad]()
        
        var instanceUri = subjectUri

        if(ontologyProperty != null)
        {
            instanceUri = pageContext.generateUri(subjectUri, ontologyProperty.name)

            quads += new Quad(context.language,  DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, instanceUri, sourceUri)
        }

        quads += new Quad(context.language, DBpediaDatasets.OntologyProperties, instanceUri, typeOntProperty, featureOntClass.uri, sourceUri)
        quads += new Quad(context.language, DBpediaDatasets.OntologyProperties, instanceUri, latOntProperty, coord.latitude.toString, sourceUri)
        quads += new Quad(context.language, DBpediaDatasets.OntologyProperties, instanceUri, lonOntProperty, coord.longitude.toString, sourceUri)
        quads += new Quad(context.language, DBpediaDatasets.OntologyProperties, instanceUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri)

        quads
    }
}
