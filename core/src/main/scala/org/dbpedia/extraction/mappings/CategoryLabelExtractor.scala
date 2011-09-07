package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations._

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor( context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends Extractor
{
    val labelProperty = context.ontology.getProperty("rdfs:label").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Category) return new Graph()

        var quads = List[Quad]()

        quads ::= new Quad(DBpediaDatasets.CategoryLabels, new IriRef(subjectUri), new IriRef(labelProperty), new LanguageLiteral(node.title.decoded, context.language.locale.getLanguage()), node.sourceUri)

        new Graph(quads)
    }
}