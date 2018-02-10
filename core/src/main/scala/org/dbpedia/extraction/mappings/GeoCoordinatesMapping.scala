package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser.{PropertyNode, TemplateNode}
import org.dbpedia.extraction.dataparser._
import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.Language

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts geo-coodinates.
 */
@SoftwareAgentAnnotation(classOf[GeoCoordinatesMapping], AnnotationType.Extractor)
class GeoCoordinatesMapping( 
  val ontologyProperty : OntologyProperty,
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
    def language : Language 
  } 
) 
extends PropertyMapping
{
  private val logger = ExtractionLogger.getLogger(getClass, context.language)

  private val geoCoordinateParser = new GeoCoordinateParser(context)
  private val singleGeoCoordinateParser = new SingleGeoCoordinateParser(context)
  private val doubleParser = new DoubleParser(context)
  private val doubleParserEn = new DoubleParser(context = new {def language : Language = Language("en")})
  private val stringParser = StringParser
  private val wikiCode = context.language.wikiCode

  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val latOntProperty = context.ontology.properties("geo:lat")
  private val lonOntProperty = context.ontology.properties("geo:long")
  private val pointOntProperty = context.ontology.properties("georss:point")
  private val featureOntClass =  context.ontology.classes("geo:SpatialThing")

  private val xsdFloat = context.ontology.datatypes("xsd:float")
  private val xsdString =  context.ontology.datatypes("xsd:string")

  private val doubleType = new Datatype("xsd:double")

  override val datasets = Set(DBpediaDatasets.OntologyPropertiesGeo)

  private val qb = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.OntologyPropertiesGeo)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    qb.setNodeRecord(node.getNodeRecord)
    qb.setSourceUri(node.sourceIri)
    qb.setSubject(subjectUri)

    extractGeoCoordinate(node) match
    {
      case Some(coord) => writeGeoCoordinate(node, coord, subjectUri)
      case None => Seq.empty
    }
  }

  private def extractGeoCoordinate(node : TemplateNode) : Option[GeoCoordinate] =
  {
    // case 1: coordinates set (all coordinates in one template property)
    if(coordinates != null)
    {
      for ( 
        coordProperty <- node.property(coordinates);
        geoCoordinate <- geoCoordinateParser.parseWithProvenance(coordProperty)
      )
      {
        return Some(geoCoordinate.value)
      }
    }

    // case 2: latitude and longitude set (all coordinates in two template properties)
    if (latitude != null && longitude != null)
    {
      for( 
        latitudeProperty <- node.property(latitude);
        longitudeProperty <- node.property(longitude);
        lat <- getSingleCoordinate(latitudeProperty, -90.0, 90.0, wikiCode);
        lon <- getSingleCoordinate(longitudeProperty, -180.0, 180.0, wikiCode)
      )
      {
        try
        {
          return Some(new GeoCoordinate(lat.value, lon.value))
        }
        catch
        {
          case ex : IllegalArgumentException  => logger.log(Level.DEBUG, "Invalid geo coordinate", ex); return None
        }
      }
    }

    // case 3: more than two latitude and longitude properties (all coordinates in more than two template properties)
    if (longitudeDegrees != null && latitudeDegrees != null)
    {
      for( 
        latDegProperty <- node.property(latitudeDegrees);
        lonDegProperty <- node.property(longitudeDegrees);
        latDeg <- doubleParser.parseWithProvenance(latDegProperty);
        lonDeg <- doubleParser.parseWithProvenance(lonDegProperty)
      )
      {
        val latMin = node.property(latitudeMinutes).flatMap(doubleParser.parseWithProvenance).getOrElse(ParseResult(0.0)).value
        val latSec = node.property(latitudeSeconds).flatMap(doubleParser.parseWithProvenance).getOrElse(ParseResult(0.0)).value
        val latDir = node.property(latitudeDirection).flatMap(stringParser.parseWithProvenance).getOrElse(ParseResult("N")).value

        val lonMin = node.property(longitudeMinutes).flatMap(doubleParser.parseWithProvenance).getOrElse(ParseResult(0.0)).value
        val lonSec = node.property(longitudeSeconds).flatMap(doubleParser.parseWithProvenance).getOrElse(ParseResult(0.0)).value
        val lonDir = node.property(longitudeDirection).flatMap(stringParser.parseWithProvenance).getOrElse(ParseResult("E")).value

        try
        {
          return Some(new GeoCoordinate(latDeg.value, latMin, latSec, latDir, lonDeg.value, lonMin, lonSec, lonDir, false))
        }
        catch
        {
          case ex : IllegalArgumentException  => logger.log(Level.DEBUG, "Invalid geo coordinate", ex); return None
        }
      }
    }

    None
  }

  private def writeGeoCoordinate(node : TemplateNode, coord : GeoCoordinate, subjectUri : String) : Seq[Quad] =
  {
    var quads = new ArrayBuffer[Quad]()
    
    var instanceUri = subjectUri
    
    val qbs = qb.clone

    if(ontologyProperty != null)
    {
      instanceUri = node.generateUri(subjectUri, ontologyProperty.name)

      qbs.setSubject(subjectUri)
      qbs.setPredicate(ontologyProperty)
      qbs.setValue(instanceUri)
      quads += qbs.getQuad
    }

    qbs.setSubject(instanceUri)
    qbs.setPredicate(typeOntProperty)
    qbs.setValue(featureOntClass.uri)
    quads += qbs.getQuad

    qbs.setSubject(instanceUri)
    qbs.setPredicate(pointOntProperty)
    qbs.setValue(coord.latitude + " " + coord.longitude)
    qbs.setDatatype(xsdString)
    quads += qbs.getQuad

    qbs.setSubject(instanceUri)
    qbs.setPredicate(latOntProperty)
    qbs.setValue(coord.latitude.toString)
    qbs.setDatatype(xsdFloat)
    quads += qbs.getQuad

    qbs.setSubject(instanceUri)
    qbs.setPredicate(lonOntProperty)
    qbs.setValue(coord.longitude.toString)
    qbs.setDatatype(xsdFloat)
    quads += qbs.getQuad

    quads
  }

  @unchecked
  private def getSingleCoordinate(coordinateProperty: PropertyNode, rangeMin: Double, rangeMax: Double, wikiCode: String ): Option[ParseResult[Double]] = {
    singleGeoCoordinateParser.parseWithProvenance(coordinateProperty) orElse doubleParser.parseWithProvenance(coordinateProperty) match {
      case Some(coordinateValue) =>
        val doubleVal = coordinateValue match{
          case ParseResult(value: SingleGeoCoordinate, lang, typ, prov) => ParseResult(
            value.toDouble,
            lang,
            typ,
            prov
          )
          case dpr: ParseResult[Double] => dpr
        }
        //Check if the coordinate is in the correct range
        if (rangeMin <= doubleVal.value && doubleVal.value <= rangeMax) {
          Some(doubleVal)
        } else if (!wikiCode.equals("en"))  {
          // Sometimes coordinates are written with the English locale (. instead of ,)
          doubleParserEn.parseWithProvenance(coordinateProperty) match {
            case Some(enCoordinateValue) =>
              if (rangeMin <= enCoordinateValue.value && enCoordinateValue.value <= rangeMax) {
                // do not return invalid coordinates either way
                Some(enCoordinateValue)
              } else None
            case None => None
          }
        } else None
      case None => None
    }
  }
}
