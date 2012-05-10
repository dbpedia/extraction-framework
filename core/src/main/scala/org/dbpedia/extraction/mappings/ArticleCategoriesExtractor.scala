package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

/**
 * Extracts links from concepts to categories using the SKOS vocabulary.
 */
class ArticleCategoriesExtractor( context : {
                                      def ontology : Ontology
                                      def language : Language } ) extends Extractor
{
    private val dctermsSubjectProperty = context.ontology.properties("dct:subject")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()
        
        val links = collectCategoryLinks(node).filter(isCategoryForArticle(_))

        val quads : List[Quad] = links.map(link =>
            new Quad(context.language, DBpediaDatasets.ArticleCategories, subjectUri, dctermsSubjectProperty, getUri(link.destination), link.sourceUri)
        )

        new Graph(quads)
    }

    private def isCategoryForArticle(linkNode : InternalLinkNode) = linkNode.destinationNodes match
    {
        case TextNode(text, _) :: Nil  => !text.startsWith(":")  // links starting wih ':' are actually only related, not the category of this article
        case _ => true
    }

    private def collectCategoryLinks(node : Node) : List[InternalLinkNode] =
    {
        node match
        {
            case linkNode : InternalLinkNode if linkNode.destination.namespace == Namespace.Category => List(linkNode)
            case _ => node.children.flatMap(collectCategoryLinks)
        }
    }

    private def getUri(destination : WikiTitle) : String =
    {
        val categoryNamespace = Namespace.Category.getName(context.language)
        context.language.resourceUri.append(categoryNamespace+':'+destination.decoded)
    }   
}