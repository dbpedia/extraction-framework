package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
class RedirectExtractor( context : {
                             def ontology : Ontology
                             def language : Language }  ) extends Extractor
{
    private val wikiPageRedirectsProperty = context.ontology.properties("wikiPageRedirects")

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
    {
        if((page.title.namespace == Namespace.Main || page.title.namespace == Namespace.Template ) && page.isRedirect)
        {
            for(destination <- page.children.collect{case InternalLinkNode(destination, _, _, _) => destination})
            {
                return Seq(new Quad(context.language, DBpediaDatasets.Redirects, subjectUri, wikiPageRedirectsProperty,
                    context.language.resourceUri.append(destination.decodedWithNamespace), page.sourceUri))
            }
        }

        Seq.empty
    }
}
