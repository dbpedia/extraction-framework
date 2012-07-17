package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts page ids of articles, e.g.
 * <http://en.wikipedia.org/wiki/Foo> <http://dbpedia.org/ontology/wikiPageID> "123456"^^<xsd:integer> .
 * 
 * FIXME: does this make sense? We should probably use the DBpedia resource URI as subject.
 */
class PageIdExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends Extractor
{
  private val language = context.language

  val wikiPageIdProperty = context.ontology.properties("wikiPageID")

  override val datasets = Set(DBpediaDatasets.PageIds)

  private val quad = QuadBuilder(context.language, DBpediaDatasets.PageIds, wikiPageIdProperty, context.ontology.datatypes("xsd:integer")) _

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    val objectLink = page.title.pageIri
    Seq(quad(objectLink, page.id.toString, page.sourceUri))
  }
}