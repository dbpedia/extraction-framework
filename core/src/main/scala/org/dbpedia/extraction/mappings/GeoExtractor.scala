package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.dataparser.{GeoCoordinate, GeoCoordinateParser}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, TemplateNode}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts geo-coodinates.
 */
class GeoExtractor( extractionContext : {
                        val ontology : Ontology
                        val redirects : Redirects  // redirects required by GeoCoordinateParser
                        val language : Language } ) extends Extractor
{
    private val geoCoordinateParser = new GeoCoordinateParser(extractionContext)

    private val typeOntProperty = extractionContext.ontology.getProperty("rdf:type").get
    private val latOntProperty = extractionContext.ontology.getProperty("geo:lat").get
    private val lonOntProperty = extractionContext.ontology.getProperty("geo:long").get
    private val pointOntProperty = extractionContext.ontology.getProperty("georss:point").get
    private val featureOntClass =  extractionContext.ontology.getClass("gml:_Feature").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        // Iterate through all root templates.
        // Not recursing into templates as these are presumed to be handled by template-based mechanisms (GeoCoordinatesMapping).
        for( templateNode @ TemplateNode(_, _, _) <- node.children;
             coordinate <- geoCoordinateParser.parse(templateNode) )
        {
            return writeGeoCoordinate(coordinate, subjectUri, node.sourceUri, pageContext)
        }

        return new Graph()
    }

    private def writeGeoCoordinate(coord : GeoCoordinate, subjectUri : String, sourceUri : String, pageContext : PageContext) : Graph =
    {
        new Graph( new Quad(extractionContext.language, DBpediaDatasets.GeoCoordinates, subjectUri, typeOntProperty, featureOntClass.uri, sourceUri) ::
                   new Quad(extractionContext.language, DBpediaDatasets.GeoCoordinates, subjectUri, latOntProperty, coord.latitude.toString, sourceUri) ::
                   new Quad(extractionContext.language, DBpediaDatasets.GeoCoordinates, subjectUri, lonOntProperty, coord.longitude.toString, sourceUri) ::
                   new Quad(extractionContext.language, DBpediaDatasets.GeoCoordinates, subjectUri, pointOntProperty, coord.latitude + " " + coord.longitude, sourceUri) :: Nil )
    }
}
