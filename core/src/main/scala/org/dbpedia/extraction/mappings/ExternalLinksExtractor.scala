package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, UriUtils}
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts links to external web pages.
 */
class ExternalLinksExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  /**
   * Don't access context directly in methods. Cache context.language for use inside methods so that
   * Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language

  val wikiPageExternalLinkProperty = context.ontology.properties("wikiPageExternalLink")

  override val datasets = Set(DBpediaDatasets.ExternalLinks)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()
    for(link <- collectExternalLinks(node);
        uri <- UriUtils.cleanLink(link.destination))
    {
      quads += new Quad(language, DBpediaDatasets.ExternalLinks, subjectUri, wikiPageExternalLinkProperty, uri, link.sourceUri, null)
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
