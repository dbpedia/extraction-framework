package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts labels to articles based on their title.
 */
class LabelExtractor( context : {
                          def ontology : Ontology
                          def language : Language } ) extends Extractor
{
    val labelProperty = context.ontology.getProperty("rdfs:label").get
    
    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()

        // TODO: use templates like {{lowercase}}, remove stuff like "(1999 film)" from title...
        val label = node.root.title.decoded
        if(label.isEmpty) return new Graph()

        new Graph(new Quad(context.language, DBpediaDatasets.Labels, subjectUri, labelProperty, label,
                           node.sourceUri, context.ontology.getDatatype("xsd:string").get ))
    }
}
