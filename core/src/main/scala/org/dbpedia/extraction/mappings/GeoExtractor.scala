package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.dataparser.{GeoCoordinate, GeoCoordinateParser}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad, IriRef, PlainLiteral}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, TemplateNode}

/**
 * Extracts geo-coodinates.
 */
class GeoExtractor(extractionContext : ExtractionContext) extends Extractor
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
      val subj = new IriRef(subjectUri)

      //TODO use typed literals?
        new Graph( new Quad(DBpediaDatasets.GeoCoordinates, subj, new IriRef(typeOntProperty), new IriRef(featureOntClass.uri), new IriRef(sourceUri)) ::
                   new Quad(DBpediaDatasets.GeoCoordinates, subj, new IriRef(latOntProperty), new PlainLiteral(coord.latitude.toString), new IriRef(sourceUri)) ::
                   new Quad(DBpediaDatasets.GeoCoordinates, subj, new IriRef(lonOntProperty),  new PlainLiteral(coord.longitude.toString), new IriRef(sourceUri)) ::
                   new Quad(DBpediaDatasets.GeoCoordinates, subj, new IriRef(pointOntProperty),  new PlainLiteral(coord.latitude + " " + coord.longitude), new IriRef(sourceUri)) :: Nil )
    }
}
