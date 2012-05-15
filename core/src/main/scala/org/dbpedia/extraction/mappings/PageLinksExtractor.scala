package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts internal links between DBpedia instances from the internal pagelinks between Wikipedia articles.
 * The page links might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms.
 */
class PageLinksExtractor( context : {
                              def ontology : Ontology
                              def language : Language }  ) extends Extractor
{
    val wikiPageWikiLinkProperty = context.ontology.properties("wikiPageWikiLink")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main) return Seq.empty
        
        val list = collectInternalLinks(node)
        
        list.map(link => new Quad(context.language, DBpediaDatasets.PageLinks, subjectUri, wikiPageWikiLinkProperty, getUri(link.destination), link.sourceUri, null))
    }

    private def collectInternalLinks(node : Node) : List[InternalLinkNode] =
    {
        node match
        {
            case linkNode : InternalLinkNode => List(linkNode)
            case _ => node.children.flatMap(collectInternalLinks)
        }
    }

    private def getUri(destination : WikiTitle) : String =
    {
        context.language.resourceUri.append(destination.decodedWithNamespace)
    }
}