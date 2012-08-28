package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts page ids to articles.
 */
class PageIdExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends Extractor
{
  private val language = context.language

  val wikiPageIdProperty = context.ontology.properties("wikiPageID")

  override val datasets = Set(DBpediaDatasets.PageIds)

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    val objectLink = page.title.pageIri

    Seq(new Quad(context.language, DBpediaDatasets.PageIds, objectLink, wikiPageIdProperty, page.id.toString, page.sourceUri, context.ontology.datatypes("xsd:integer")))
  }
}