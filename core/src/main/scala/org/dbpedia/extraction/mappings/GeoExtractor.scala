package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.dataparser.{GeoCoordinate, GeoCoordinateParser}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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

  private val qb = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.GeoCoordinates)

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
      qb.setSourceUri(templateNode.sourceIri)
      qb.setNodeRecord(templateNode.getNodeRecord)
      qb.setExtractor(ExtractorRecord(
        this.softwareAgentAnnotation,
        coordinate.provenance.toList,
        None, None, Some(templateNode.title),
        templateNode.containedTemplateNames()
      ))

      val quads = new ArrayBuffer[Quad]()
      qb.setSubject(subjectUri)
      qb.setPredicate(typeOntProperty)
      qb.setValue(featureOntClass.uri)
      quads += qb.getQuad

      qb.setPredicate(latOntProperty)
      qb.setValue(coordinate.value.latitude.toString)
      quads += qb.getQuad

      qb.setPredicate(lonOntProperty)
      qb.setValue(coordinate.value.longitude.toString)
      quads += qb.getQuad

      qb.setPredicate(pointOntProperty)
      qb.setValue(coordinate.value.latitude + " " + coordinate.value.latitude)
      quads += qb.getQuad
      quads
    }

    Seq.empty
  }
}
