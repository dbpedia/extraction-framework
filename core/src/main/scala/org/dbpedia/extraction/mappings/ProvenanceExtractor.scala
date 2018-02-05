package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.language.reflectiveCalls

/**
 * Extracts links to the article revision that the data was extracted from, e.g.
 * <http://dbpedia.org/resource/Foo> <http://www.w3.org/ns/prov#wasDerivedFrom> <http://en.wikipedia.org/wiki/Foo?oldid=123456> .
 */
@SoftwareAgentAnnotation(classOf[ProvenanceExtractor], AnnotationType.Extractor)
class ProvenanceExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
  private val propString = "http://www.w3.org/ns/prov#wasDerivedFrom"

  override val datasets = Set(DBpediaDatasets.RevisionUris)

  private val qb = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.RevisionUris, propString)

  override def extract(page: WikiPage, subjectUri: String): Seq[Quad] = {
    qb.setNodeRecord(page.getNodeRecord)
    qb.setExtractor(this.softwareAgentAnnotation)
    qb.setSubject(subjectUri)
    qb.setValue(page.sourceIri)
    qb.setSourceUri(page.sourceIri)
    Seq(qb.getQuad)
  }
}
