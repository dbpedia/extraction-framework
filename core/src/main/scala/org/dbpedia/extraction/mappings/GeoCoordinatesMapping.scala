package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import java.util.logging.{Logger, Level}

/**
 * Extracts geo-coodinates.
 */
class GeoCoordinatesMapping( ontologyProperty : OntologyProperty,
                             coordinates : String,
                             latitude : String,
                             longitude : String,
                             longitudeDegrees : String,
                             longitudeMinutes : String,
                             longitudeSeconds : String,
                             longitudeDirection : String,
                             latitudeDegrees : String,
                             latitudeMinutes : String,
                             latitudeSeconds : String,
                             latitudeDirection : String,
                             extractionContext : ExtractionContext ) extends PropertyMapping
{
    private val logger = Logger.getLogger(classOf[GeoCoordinatesMapping].getName) 

    private val geoCoordinateParser = new GeoCoordinateParser(extractionContext)
    private val doubleParser = new DoubleParser(extractionContext)
    private val stringParser = StringParser

    private val typeOntProperty = extractionContext.ontology.getProperty("rdf:type").get
    private val latOntProperty = extractionContext.ontology.getProperty("geo:lat").get
    private val lonOntProperty = extractionContext.ontology.getProperty("geo:long").get
    private val pointOntProperty = extractionContext.ontology.getProperty("georss:point").get
    private val featureOntClass =  extractionContext.ontology.getClass("gml:_Feature").get

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        extractGeoCoordinate(node) match
        {
            case Some(coord) => writeGeoCoordinate(node, coord, subjectUri, node.sourceUri, pageContext)
            case None => new Graph()
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

        return None
    }

    private def writeGeoCoordinate(node : TemplateNode, coord : GeoCoordinate, subjectUri : String, sourceUri : String, pageContext : PageContext) : Graph =
    {
        var quads = List[Quad]()
        var instanceUri = subjectUri

        if(ontologyProperty != null)
        {
            instanceUri = pageContext.generateUri(subjectUri, ontologyProperty.name)

            quads ::= new Quad(extractionContext.language,  DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, instanceUri, sourceUri)
        }

        quads ::= new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, instanceUri, typeOntProperty, featureOntClass.uri, sourceUri)
        quads ::= new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, instanceUri, latOntProperty, coord.latitude.toString, sourceUri)
        quads ::= new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, instanceUri, lonOntProperty, coord.longitude.toString, sourceUri)
        quads ::= new Quad(extractionContext.language, DBpediaDatasets.OntologyProperties, instanceUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri)

        return new Graph(quads)
    }
}
