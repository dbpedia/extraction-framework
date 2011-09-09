package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad, IriRef}
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
    private val wikiPageRedirectsProperty = context.ontology.getProperty("wikiPageRedirects")
                                            .getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageRedirects' does not exist in DBpedia Ontology."))

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if((page.title.namespace == WikiTitle.Namespace.Main || page.title.namespace == WikiTitle.Namespace.Template ) && page.isRedirect)
        {
            for(destination <- page.children.collect{case InternalLinkNode(destination, _, _, _) => destination})
            {
              return new Graph(
                new Quad(DBpediaDatasets.Redirects,
                  new IriRef(subjectUri),
                  new IriRef(wikiPageRedirectsProperty),
                  new IriRef(OntologyNamespaces.getResource(destination.encodedWithNamespace, context.language)),  //OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE + destination.encodedWithNamespace
                  new IriRef(page.sourceUri))
                )
            }
        }

        new Graph()
    }
}
