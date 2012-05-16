package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts page ids to articles.
 */
class PageIdExtractor( context : {
                           def ontology : Ontology
                           def language : Language }  ) extends Extractor
{
    private val language = context.language

    val wikiPageIdProperty = context.ontology.properties("wikiPageID")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        val objectLink = node.root.title.pageIri

        Seq(new Quad(context.language, DBpediaDatasets.PageIds, objectLink, wikiPageIdProperty,
                            node.id.toString, node.sourceUri, context.ontology.datatypes("xsd:integer")))
    }
}