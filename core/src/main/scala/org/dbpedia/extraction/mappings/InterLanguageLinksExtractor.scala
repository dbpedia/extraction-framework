package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.InterLanguageLinksExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer


/**
 * Extracts interwiki links and creates owl:sameAs triples
 */
class InterLanguageLinksExtractor(context: { def ontology : Ontology; def language : Language }) extends Extractor
{
    private val intLinksSet = InterLanguageLinksExtractorConfig.intLinksMap(context.language.wikiCode)

    // FIXME: owl:sameAs is too strong, many linked pages are about very different subjects
    private val interLanguageLinksProperty = context.ontology.properties("owl:sameAs")
    //context.ontology.getProperty("interLanguageLinks")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main) return Seq.empty
        
        var quads = new ArrayBuffer[Quad]()

        for(node <- node.children.reverse)
        {
          node match {
            case link: InterWikiLinkNode =>
              val dst = link.destination
              if (dst.isInterLanguageLink && intLinksSet.contains(dst.language.wikiCode)) {
                val lang = dst.language
                quads += new Quad(context.language, DBpediaDatasets.SameAs, subjectUri, interLanguageLinksProperty,
                  lang.resourceUri.append(dst.decodedWithNamespace), link.sourceUri, null)
              }
            case _ => // ignore
          }
        }
        
        quads
    }

}