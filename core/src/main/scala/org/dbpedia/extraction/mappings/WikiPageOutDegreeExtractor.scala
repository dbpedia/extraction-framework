package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts the number of external links to DBpedia instances from the internal page links between
 * Wikipedia articles. The Out Degree might be useful for structural analysis, data mining
 * or for ranking DBpedia instances using Page Rank or similar algorithms. In Degree cannot be
 * calculated at extraction time but with a post processing step from the PageLinks dataset
 */
@SoftwareAgentAnnotation(classOf[WikiPageOutDegreeExtractor], AnnotationType.Extractor)
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
  private val qb = QuadBuilder(context.language, DBpediaDatasets.OutDegree, wikiPageOutDegreeProperty, nonNegativeInteger)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) 
        return Seq.empty


    val ìnternalLinks = ExtractorUtils.collectInternalLinksFromNode(node)
    qb.setNodeRecord(node.getNodeRecord)
    qb.setSourceUri(node.sourceIri)
    qb.setSubject(subjectUri)
    qb.setValue(ìnternalLinks.size.toString)

    Seq(qb.getQuad)
  }
}
