package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
class RedirectExtractor( context : {
                             def ontology : Ontology
                             def language : Language }  ) extends Extractor
{
    private val wikiPageRedirectsProperty = context.ontology.properties("wikiPageRedirects")

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if((page.title.namespace == Namespace.Main || page.title.namespace == Namespace.Template ) && page.isRedirect)
        {
            for(destination <- page.children.collect{case InternalLinkNode(destination, _, _, _) => destination})
            {
                return new Graph(new Quad(context.language, DBpediaDatasets.Redirects, subjectUri, wikiPageRedirectsProperty,
                    OntologyNamespaces.getResource(destination.encodedWithNamespace, context.language), page.sourceUri))
                    //OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE + destination.encodedWithNamespace
            }
        }

        new Graph()
    }
}
