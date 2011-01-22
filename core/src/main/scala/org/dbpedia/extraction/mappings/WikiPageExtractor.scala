package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

/**
 * Extracts links to corresponding Articles in Wikipedia.
 */
class WikiPageExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val foafPageProperty = extractionContext.ontology.getProperty("foaf:page").getOrElse(throw new Exception("Property 'foaf:page' not found"))
    private val dcLanguageProperty = extractionContext.ontology.getProperty("dc:language").getOrElse(throw new Exception("Property 'dc:language' not found"))

    private val foafPrimaryTopicProperty = extractionContext.ontology.getProperty("foaf:primaryTopic").getOrElse(throw new Exception("Property 'foaf:primaryTopic' not found"))

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        val objectLink = "http://" + extractionContext.language.wikiCode + ".wikipedia.org/wiki/" + node.root.title.encoded

        quads ::= new Quad(extractionContext, DBpediaDatasets.LinksToWikipediaArticle, subjectUri, foafPageProperty,  objectLink, node.sourceUri)
        quads ::= new Quad(extractionContext, DBpediaDatasets.LinksToWikipediaArticle, objectLink, dcLanguageProperty,  extractionContext.language.wikiCode, node.sourceUri)

        quads ::= new Quad(extractionContext, DBpediaDatasets.LinksToWikipediaArticle, objectLink, foafPrimaryTopicProperty, subjectUri, node.sourceUri)

        new Graph(quads)
    }
}
