package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad, TypedLiteral, IriRef}

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

        quads ::= new Quad(DBpediaDatasets.CategoryLabels, new IriRef(subjectUri), new IriRef(labelProperty), new TypedLiteral(node.title.decoded, new Datatype("xsd:string")), new IriRef(node.sourceUri))

        new Graph(quads)
    }
}