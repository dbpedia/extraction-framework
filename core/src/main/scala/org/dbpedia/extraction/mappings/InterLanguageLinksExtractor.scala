package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.InterLanguageLinksExtractorConfig
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language


/**
 * Extracts interwiki links and creates owl:sameAs triples
 */
class InterLanguageLinksExtractor(context: { def ontology : Ontology; def language : Language }) extends Extractor
{
    private val intLinksSet = InterLanguageLinksExtractorConfig.intLinksMap(context.language.wikiCode)

    // FIXME: owl:sameAs is too strong, many linked pages are about very different subjects
    private val interLanguageLinksProperty = context.ontology.properties("owl:sameAs")
    //context.ontology.getProperty("interLanguageLinks")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        for(node <- node.children.reverse)
        {
          node match {
            case link: InterWikiLinkNode =>
              val dst = link.destination
              if (dst.isInterLanguageLink && intLinksSet.contains(dst.language.wikiCode)) {
                val lang = dst.language
                quads ::= new Quad(context.language, DBpediaDatasets.SameAs, subjectUri, interLanguageLinksProperty,
                  OntologyNamespaces.getResource(dst.encodedWithNamespace, lang), link.sourceUri, null)
              }
            case _ => // ignore
          }
        }
        
        new Graph(quads)
    }

}