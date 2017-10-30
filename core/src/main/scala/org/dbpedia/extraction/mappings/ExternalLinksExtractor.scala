package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.iri.UriUtils

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts links to external web pages.
 */
@SoftwareAgentAnnotation(classOf[ExternalLinksExtractor], AnnotationType.Extractor)
class ExternalLinksExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  val wikiPageExternalLinkProperty = context.ontology.properties("wikiPageExternalLink")

  override val datasets = Set(DBpediaDatasets.ExternalLinks)

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) 
        return Seq.empty

    var quads = new ArrayBuffer[Quad]()
    for(link <- collectExternalLinks(node);
        uri <- UriUtils.cleanLink(link.destination))
    {
      quads += new Quad(context.language, DBpediaDatasets.ExternalLinks, subjectUri, wikiPageExternalLinkProperty, uri, link.sourceIri, null)
    }
    
    quads
  }

  private def collectExternalLinks(node : Node) : List[ExternalLinkNode] =
  {
    node match
    {
      case linkNode : ExternalLinkNode => List(linkNode)
      case _ => node.children.flatMap(collectExternalLinks)
    }
  }
}
