package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts links to the article revision that the data was extracted from, e.g.
 * <http://dbpedia.org/resource/Foo> <http://www.w3.org/ns/prov#wasDerivedFrom> <http://en.wikipedia.org/wiki/Foo?oldid=123456> .
 */
class RevisionUriExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends Extractor
{
  private val derivedFromProperty = "http://www.w3.org/ns/prov#wasDerivedFrom"

  override val datasets = Set(DBpediaDatasets.RevisionUris)

  private val quad = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.RevisionUris, derivedFromProperty, null) _

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    Seq(quad(subjectUri, page.sourceUri, page.sourceUri))
  }
}