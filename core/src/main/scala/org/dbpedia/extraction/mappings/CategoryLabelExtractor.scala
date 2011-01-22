package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    require(Set("en").contains(language))

    val labelProperty = extractionContext.ontology.getProperty("rdfs:label").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Category) return new Graph()

        var quads = List[Quad]()

        quads ::= new Quad(extractionContext, DBpediaDatasets.CategoryLabels, subjectUri, labelProperty, node.title.decoded, node.sourceUri, new Datatype("xsd:string"))

        new Graph(quads)
    }
}