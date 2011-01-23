package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, InternalLinkNode, Node}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

/**
 * Extracts links from concepts to categories using the SKOS vocabulary.
 */
class ArticleCategoriesExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    private val dctermsSubjectProperty = extractionContext.ontology.getProperty("dct:subject").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        val list = collectInternalLinks(node)
        list.foreach(link => {
            quads ::= new Quad(extractionContext, DBpediaDatasets.ArticleCategories, subjectUri, dctermsSubjectProperty, getUri(link.destination), link.sourceUri)
        })
        new Graph(quads)
    }

    private def collectInternalLinks(node : Node) : List[InternalLinkNode] =
    {
        node match
        {
            case linkNode : InternalLinkNode if linkNode.destination.namespace == WikiTitle.Namespace.Category => List(linkNode)
            case _ => node.children.flatMap(collectInternalLinks)
        }
    }

    private def getUri(destination : WikiTitle) : String =
    {
        val categoryNamespace = Namespaces.getNameForNamespace(extractionContext.language, WikiTitle.Namespace.Category)

        OntologyNamespaces.getUri(categoryNamespace + ":" + destination.encoded, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE)
    }   
}