package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad, IriRef}
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, InternalLinkNode, Node}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

/**
 * Extracts links from concepts to categories using the SKOS vocabulary.
 */
class ArticleCategoriesExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    require(Set("en").contains(language))

    private val dctermsSubjectProperty = extractionContext.ontology.getProperty("dct:subject").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        val list = collectInternalLinks(node)
        list.foreach(link => {
            quads ::= new Quad(DBpediaDatasets.ArticleCategories, new IriRef(subjectUri), new IriRef(dctermsSubjectProperty), getUri(link.destination), new IriRef(link.sourceUri))
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

    private def getUri(destination : WikiTitle) : IriRef =
    {
        val categoryNamespace = Namespaces.getNameForNamespace(extractionContext.language, WikiTitle.Namespace.Category)

        new IriRef(OntologyNamespaces.getUri(categoryNamespace + ":" + destination.encoded, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE))
    }   
}