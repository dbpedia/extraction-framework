package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts revision ids to articles.
 */
class RevisionIdExtractor( context : {
                               def ontology : Ontology
                               def language : Language }  ) extends Extractor
{
    private val wikiPageRevisionIDProperty = context.ontology.getProperty("wikiPageRevisionID")
                                             .getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageRevisionID' does not exist in DBpedia Ontology."))


    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        val objectLink = "http://" + context.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded

        new Graph(new Quad(DBpediaDatasets.Revisions, new IriRef(objectLink), new IriRef(wikiPageRevisionIDProperty),
            new TypedLiteral(node.revision.toString, extractionContext.ontology.getDatatype("xsd:integer").get), new IriRef(node.sourceUri) ))
    }
}