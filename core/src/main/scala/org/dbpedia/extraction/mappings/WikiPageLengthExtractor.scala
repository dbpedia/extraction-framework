package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts the number of characters in a wikipedia page
 */
@SoftwareAgentAnnotation(classOf[WikiPageLengthExtractor], AnnotationType.Extractor)
class WikiPageLengthExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends WikiPageExtractor
{
  val wikiPageLengthProperty = context.ontology.properties("wikiPageLength")
  val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  override val datasets = Set(DBpediaDatasets.PageLength)

  private val qb = QuadBuilder(context.language, DBpediaDatasets.PageLength, wikiPageLengthProperty, nonNegativeInteger)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(page : WikiPage, subjectUri : String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) 
        return Seq.empty

    qb.setNodeRecord(page.getNodeRecord)
    qb.setSourceUri(page.sourceIri)
    qb.setSubject(subjectUri)
    qb.setValue(page.source.length.toString)

    Seq(qb.getQuad)
  }
}
