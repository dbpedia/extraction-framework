package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor( context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends Extractor
{
    val labelProperty = context.ontology.properties("rdfs:label")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Category) return new Graph()

        var quads = List[Quad]()

        quads ::= new Quad(context.language, DBpediaDatasets.CategoryLabels, subjectUri, labelProperty, node.title.decoded, node.sourceUri, new Datatype("xsd:string"))

        new Graph(quads)
    }
}