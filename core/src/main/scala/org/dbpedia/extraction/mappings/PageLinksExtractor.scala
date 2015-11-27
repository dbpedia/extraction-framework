package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import scala.language.reflectiveCalls

/**
 * Extracts internal links between DBpedia instances from the internal page links between
 * Wikipedia articles. The page links might be useful for structural analysis, data mining 
 * or for ranking DBpedia instances using Page Rank or similar algorithms.
 */
class PageLinksExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  val wikiPageWikiLinkProperty = context.ontology.properties("wikiPageWikiLink")

  override val datasets = Set(DBpediaDatasets.PageLinks)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) 
        return Seq.empty
    
    val list = ExtractorUtils.collectInternalLinksFromNode(node)

    list.map(link => new Quad(context.language, DBpediaDatasets.PageLinks, subjectUri, wikiPageWikiLinkProperty, getUri(link.destination), link.sourceUri, null))
  }

  private def getUri(destination : WikiTitle) : String =
  {
    context.language.resourceUri.append(destination.decodedWithNamespace)
  }

}

