package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts links to the article revision that the data was extracted from.
 * http://www.w3.org/ns/prov#wasDerivedFrom
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

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    Seq(quad(subjectUri, node.sourceUri, node.sourceUri))
  }
}