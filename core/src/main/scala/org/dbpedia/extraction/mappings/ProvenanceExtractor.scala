package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

/**
 * Extracts links to the article revision that the data was extracted from, e.g.
 * <http://dbpedia.org/resource/Foo> <http://www.w3.org/ns/prov#wasDerivedFrom> <http://en.wikipedia.org/wiki/Foo?oldid=123456> .
 */
class ProvenanceExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
  private val derivedFromProperty = "http://www.w3.org/ns/prov#wasDerivedFrom"

  override val datasets = Set(DBpediaDatasets.RevisionUris)

  private val quad = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.RevisionUris, derivedFromProperty, null) _

  override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    Seq(quad(subjectUri, page.title.pageIri, page.title.pageIri))
  }
}