package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls


/**
 * Extracts interwiki links
 */
@SoftwareAgentAnnotation(classOf[InterLanguageLinksExtractor], AnnotationType.Extractor)
class InterLanguageLinksExtractor(context: { def ontology : Ontology; def language : Language }) extends PageNodeExtractor
{
  private val interLanguageLinksProperty = context.ontology.properties("wikiPageInterLanguageLink")

  override val datasets = Set(DBpediaDatasets.InterLanguageLinks)
  
  private val namespaces = if (context.language == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata
    else Set(Namespace.Main, Namespace.Template, Namespace.Category)
  
  private val quad = QuadBuilder.apply(context.language, DBpediaDatasets.InterLanguageLinks, interLanguageLinksProperty, null) _

  override def extract(page : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if (! namespaces.contains(page.title.namespace)) return Seq.empty
    
    var quads = new ArrayBuffer[Quad]()

    for (node <- page.children) { // was page.children.reverse - why?
      node match {
        case link: InterWikiLinkNode => {
          val dst = link.destination
          if (dst.isInterLanguageLink) {
            val dstLang = dst.language
            quads += quad(subjectUri, dstLang.resourceUri.append(dst.decodedWithNamespace), link.sourceIri)
          }
        }
        case _ => // ignore
      }
    }
    
    quads
  }

}
