package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts labels to articles based on their title.
 */
class LabelExtractor( extractionContext : {
                          val ontology : Ontology
                          val language : Language } ) extends Extractor
{
    val labelProperty = extractionContext.ontology.getProperty("rdfs:label").get
    
    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        val label = node.root.title.decoded
        if(label.isEmpty) return new Graph()

        new Graph(new Quad(extractionContext.language, DBpediaDatasets.Labels, subjectUri, labelProperty, label,
                           node.sourceUri, extractionContext.ontology.getDatatype("xsd:string").get ))
    }
}
