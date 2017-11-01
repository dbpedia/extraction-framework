package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.dataparser.{GeoCoordinate, GeoCoordinateParser}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts geo-coodinates.
 */
@SoftwareAgentAnnotation(classOf[GeoExtractor], AnnotationType.Extractor)
class GeoExtractor( 
  context : {
    def ontology : Ontology
    def redirects : Redirects  // redirects required by GeoCoordinateParser
    def language : Language 
  } 
) 
extends PageNodeExtractor
{
  private val geoCoordinateParser = new GeoCoordinateParser(context)

  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val latOntProperty = context.ontology.properties("geo:lat")
  private val lonOntProperty = context.ontology.properties("geo:long")
  private val pointOntProperty = context.ontology.properties("georss:point")
  private val featureOntClass =  context.ontology.classes("geo:SpatialThing")

  override val datasets = Set(DBpediaDatasets.GeoCoordinates)

  override def extract(page : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if (page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) 
      return Seq.empty
    
    // Iterate through all root templates.
    // Not recursing into templates as these are presumed to be handled by template-based mechanisms (GeoCoordinatesMapping).
    for(
      templateNode @ TemplateNode(_, _, _, _) <- page.children;
      coordinate <- geoCoordinateParser.parseWithProvenance(templateNode)
    )
    {
      return writeGeoCoordinate(coordinate.value, subjectUri, page.sourceIri)
    }

    Seq.empty
  }

  private def writeGeoCoordinate(coord : GeoCoordinate, subjectUri : String, sourceUri : String) : Seq[Quad] =
  {
    val quads = new ArrayBuffer[Quad]()
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, typeOntProperty, featureOntClass.uri, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, latOntProperty, coord.latitude.toString, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, lonOntProperty, coord.longitude.toString, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri)
    quads
  }
}
