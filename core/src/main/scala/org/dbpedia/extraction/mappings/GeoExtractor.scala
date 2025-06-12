package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.util.Try

/**
 * Extracts geo-coordinates from Wikipedia pages.
 *
 * This extractor processes template nodes to find geographic coordinates
 * and converts them into RDF quads for the knowledge graph.
 */
class GeoExtractor(
                    context : {
                      def ontology : Ontology
                      def redirects : Redirects
                      def language : Language
                    }
                  )
  extends PageNodeExtractor {

  // Get ontology properties and classes
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val latOntProperty = context.ontology.properties("geo:lat")
  private val lonOntProperty = context.ontology.properties("geo:long")
  private val pointOntProperty = context.ontology.properties("georss:point")
  private val featureOntClass = context.ontology.classes("geo:SpatialThing")

  // Use the correct dataset type - check what other extractors use
  override val datasets = Set(DBpediaDatasets.GeoCoordinates)

  /**
   * Extract geo-coordinates from a Wikipedia page
   *
   * @param page The page node to process
   * @param subjectUri The URI of the subject resource
   * @return Sequence of RDF quads representing the extracted geo-coordinates
   */
  override def extract(page : PageNode, subjectUri : String) : Seq[Quad] = {
    // Only process main namespace pages and commons metadata
    if (page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title))
      return Seq.empty

    val quads = new ArrayBuffer[Quad]()

    // Look for coordinate templates in the page
    for (templateNode @ TemplateNode(title, _, _, _) <- page.children) {
      val templateName = title.decoded.toLowerCase

      // Check for common coordinate templates
      if (isCoordinateTemplate(templateName)) {
        // Try to extract coordinates from template parameters
        extractCoordinatesFromTemplate(templateNode) match {
          case Some((lat, lon)) =>
            quads ++= createGeoCoordinateQuads(lat, lon, subjectUri, page.sourceIri)
          case None => // Continue to next template
        }
      }
    }

    quads.toSeq
  }

  /**
   * Check if a template is likely to contain coordinates
   */
  private def isCoordinateTemplate(templateName: String): Boolean = {
    templateName.contains("coord") ||
      templateName.contains("location") ||
      templateName.contains("infobox") ||
      templateName.contains("geobox")
  }

  /**
   * Extract latitude and longitude from template parameters
   */
  private def extractCoordinatesFromTemplate(template: TemplateNode): Option[(Double, Double)] = {
    val params = template.children.collect { case p: PropertyNode => p }

    // Look for common coordinate parameter patterns
    val latParams = List("lat", "latitude", "lat_d", "lat_deg")
    val lonParams = List("lon", "long", "longitude", "lon_d", "lon_deg")

    val latitude = findCoordinateValue(params, latParams)
    val longitude = findCoordinateValue(params, lonParams)

    (latitude, longitude) match {
      case (Some(lat), Some(lon)) if isValidCoordinate(lat, lon) => Some((lat, lon))
      case _ => None
    }
  }

  /**
   * Find coordinate value from template parameters
   */
  private def findCoordinateValue(params: List[PropertyNode], paramNames: List[String]): Option[Double] = {
    for (paramName <- paramNames) {
      params.find(_.key.toLowerCase.contains(paramName)) match {
        case Some(param) =>
          parseCoordinate(param.children.map(_.toPlainText).mkString.trim) match {
            case Some(value) => return Some(value)
            case None => // Continue searching
          }
        case None => // Continue searching
      }
    }
    None
  }

  /**
   * Parse coordinate string to double value
   */
  private def parseCoordinate(coordStr: String): Option[Double] = {
    Try {
      // Clean up the coordinate string
      val cleaned = coordStr.replaceAll("[^0-9.\\-+]", "")
      if (cleaned.nonEmpty) {
        cleaned.toDouble
      } else {
        throw new NumberFormatException("Empty coordinate")
      }
    }.toOption
  }

  /**
   * Validate that coordinates are within valid ranges
   */
  private def isValidCoordinate(lat: Double, lon: Double): Boolean = {
    lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0
  }

  /**
   * Convert coordinates into RDF quads
   */
  private def createGeoCoordinateQuads(lat: Double, lon: Double, subjectUri: String, sourceUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    // Add type information
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri,
      typeOntProperty, featureOntClass.uri, sourceUri)

    // Add coordinate components
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri,
      latOntProperty, lat.toString, sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri,
      lonOntProperty, lon.toString, sourceUri)

    // Add combined point representation
    quads += new Quad(context.language, DBpediaDatasets.GeoCoordinates, subjectUri,
      pointOntProperty, s"$lat $lon", sourceUri)

    quads.toSeq
  }
}