package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts page ids of articles, e.g.
 * <http://dbpedia.org/resource/Foo> <http://dbpedia.org/ontology/wikiPageID> "123456"^^<xsd:integer> .
 */
class PageIdExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends Extractor
{
  private val wikiPageIdProperty = context.ontology.properties("wikiPageID")

  override val datasets = Set(DBpediaDatasets.PageIds)

  private val quad = QuadBuilder(context.language, DBpediaDatasets.PageIds, wikiPageIdProperty, context.ontology.datatypes("xsd:integer")) _

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    Seq(quad(subjectUri, page.id.toString, page.sourceUri))
  }
}