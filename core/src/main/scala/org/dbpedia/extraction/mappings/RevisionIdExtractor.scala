package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.language.reflectiveCalls

/**
 * Extracts revision ids of articles, e.g.
 * <http://dbpedia.org/resource/Foo> <http://dbpedia.org/ontology/wikiPageRevisionID> "123456"^^<xsd:integer> .
 */
@SoftwareAgentAnnotation(classOf[RevisionIdExtractor], AnnotationType.Extractor)
class RevisionIdExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
  private val wikiPageRevisionIdProperty = context.ontology.properties("wikiPageRevisionID")

  override val datasets = Set(DBpediaDatasets.RevisionIds)

  private val quad = QuadBuilder(context.language, DBpediaDatasets.RevisionIds, wikiPageRevisionIdProperty, context.ontology.datatypes("xsd:integer")) _

  override def extract(page: WikiPage, subjectUri: String): Seq[Quad] = {
    Seq(quad(subjectUri, page.revision.toString, page.sourceIri))
  }
}