package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.wikiparser._

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
class RedirectExtractor(extractionContext : ExtractionContext) extends Extractor
{
    val wikiPageRedirectsProperty = extractionContext.ontology.getProperty("wikiPageRedirects")
                                   .getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageRedirects' does not exist in DBpedia Ontology."))

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if((page.title.namespace == WikiTitle.Namespace.Main || page.title.namespace == WikiTitle.Namespace.Template ) && page.isRedirect)
        {
            for(destination <- page.children.collect{case InternalLinkNode(destination, _, _) => destination})
            {
                return new Graph(new Quad(extractionContext, DBpediaDatasets.Redirects, subjectUri, wikiPageRedirectsProperty,
                    OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE + destination.encodedWithNamespace, page.sourceUri))
            }
        }

        new Graph()
    }
}
