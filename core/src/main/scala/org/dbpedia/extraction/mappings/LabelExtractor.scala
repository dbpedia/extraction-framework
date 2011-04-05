package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad, IriRef, PlainLiteral}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

/**
 * Extracts labels to articles based on their title.
 */
class LabelExtractor(extractionContext : ExtractionContext) extends Extractor
{
    val labelProperty = extractionContext.ontology.getProperty("rdfs:label").get
    
    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        val label = node.root.title.decoded
        if(label.isEmpty) return new Graph()

        new Graph(new Quad(DBpediaDatasets.Labels, new IriRef(subjectUri), new IriRef(labelProperty), new PlainLiteral(label),
                            new IriRef(node.sourceUri) ))
    }
}
