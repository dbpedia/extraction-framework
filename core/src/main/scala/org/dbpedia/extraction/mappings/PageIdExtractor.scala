package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.language.reflectiveCalls

/**
 * Extracts page ids of articles, e.g.
 * <http://dbpedia.org/resource/Foo> <http://dbpedia.org/ontology/wikiPageID> "123456"^^<xsd:integer> .
 */
@SoftwareAgentAnnotation(classOf[PageIdExtractor], AnnotationType.Extractor)
class PageIdExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
  private val wikiPageIdProperty = context.ontology.properties("wikiPageID")

  override val datasets = Set(DBpediaDatasets.PageIds)

  private val quad = QuadBuilder(context.language, DBpediaDatasets.PageIds, wikiPageIdProperty, context.ontology.datatypes("xsd:integer")) _

  override def extract(page: WikiPage, subjectUri: String): Seq[Quad] = {
    Seq(quad(subjectUri, page.id.toString, page.sourceIri))
  }
}