package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer


/**
 * Extracts interwiki links
 */
class InterLanguageLinksExtractor(context: { def ontology : Ontology; def language : Language }) extends Extractor
{
  private val interLanguageLinksProperty = context.ontology.properties("interLanguageLinks")

  override val datasets = Set(DBpediaDatasets.InterLanguageLinks)

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if (page.title.namespace != Namespace.Main) return Seq.empty
    
    var quads = new ArrayBuffer[Quad]()

    for (node <- page.children) { // was page.children.reverse - why?
      node match {
        case link: InterWikiLinkNode => {
          val dst = link.destination
          if (dst.isInterLanguageLink) {
            val dstLang = dst.language
            quads += new Quad(context.language, DBpediaDatasets.InterLanguageLinks, subjectUri, interLanguageLinksProperty, dstLang.resourceUri.append(dst.decodedWithNamespace), link.sourceUri)
          }
        }
        case _ => // ignore
      }
    }
    
    quads
  }

}