package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{QuadBuilder, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import org.dbpedia.extraction.util.StringUtils._

/**
 * Extracts the number of external links to DBpedia instances from the internal page links between
 * Wikipedia articles. The Out Degree might be useful for structural analysis, data mining
 * or for ranking DBpedia instances using Page Rank or similar algorithms. In Degree cannot be
 * calculated at extraction time but with a post processing step from the PageLinks dataset
 */
class WikiPageOutDegreeExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  val wikiPageOutDegreeProperty = context.ontology.properties("wikiPageOutDegree")
  val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  override val datasets = Set(DBpediaDatasets.OutDegree)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty
    
    val list = PageLinksExtractor.collectInternalLinks(node)

    Seq(new Quad(context.language, DBpediaDatasets.OutDegree, subjectUri, wikiPageOutDegreeProperty, list.size.toString, node.sourceUri, nonNegativeInteger) )
  }
}