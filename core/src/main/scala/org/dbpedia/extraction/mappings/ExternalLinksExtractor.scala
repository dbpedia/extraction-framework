package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph}
import org.dbpedia.extraction.destinations.{Quad}
import org.dbpedia.extraction.wikiparser.{WikiTitle, PageNode, ExternalLinkNode, Node}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}

/**
 * Extracts links to external web pages.
 */
class ExternalLinksExtractor( extractionContext : {
                                  val ontology : Ontology
                                  val language : Language } ) extends Extractor
{
    val wikiPageExternalLinkProperty = extractionContext.ontology.getProperty("wikiPageExternalLink")
                                       .getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageExternalLink' does not exist in DBpedia Ontology."))

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        var quads = List[Quad]()
        for(link <- collectExternalLinks(node);
            uri <- UriUtils.cleanLink(link.destination))
        {
            try
            {
                quads ::= new Quad(extractionContext.language, DBpediaDatasets.ExternalLinks, subjectUri, wikiPageExternalLinkProperty,
                    uri, link.sourceUri, null)
            }
            catch
            {
                case e : Exception => //TODO log
            }
        }
        new Graph(quads)
    }

    private def collectExternalLinks(node : Node) : List[ExternalLinkNode] =
    {
        node match
        {
            case linkNode : ExternalLinkNode => List(linkNode)
            case _ => node.children.flatMap(collectExternalLinks)
        }
    }
}
