package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts links to corresponding Articles in Wikipedia.
 */
class WikiPageExtractor( context : {
                            def ontology : Ontology
                            def language : Language } ) extends Extractor
{
    private val foafPageProperty = context.ontology.getProperty("foaf:page").getOrElse(throw new Exception("Property 'foaf:page' not found"))
    private val dcLanguageProperty = context.ontology.getProperty("dc:language").getOrElse(throw new Exception("Property 'dc:language' not found"))

    private val foafPrimaryTopicProperty = context.ontology.getProperty("foaf:primaryTopic").getOrElse(throw new Exception("Property 'foaf:primaryTopic' not found"))

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        val objectLink = "http://" + context.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded

        quads ::= new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, subjectUri, foafPageProperty,  objectLink, node.sourceUri)
        quads ::= new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, objectLink, dcLanguageProperty,  context.language.wikiCode, node.sourceUri)

        quads ::= new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, objectLink, foafPrimaryTopicProperty, subjectUri, node.sourceUri)

        new Graph(quads)
    }
}
