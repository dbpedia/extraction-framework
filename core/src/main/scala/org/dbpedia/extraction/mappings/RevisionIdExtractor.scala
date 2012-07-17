package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts revision ids of articles, e.g.
 * <http://en.wikipedia.org/wiki/Foo> <http://dbpedia.org/ontology/wikiPageRevisionID> "123456"^^<xsd:integer> .
 * 
 * FIXME: this doesn't make sense.
 * 
 */
class RevisionIdExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends Extractor
{
  private val wikiPageRevisionIdProperty = context.ontology.properties("wikiPageRevisionID")

  override val datasets = Set(DBpediaDatasets.RevisionIds)
  
  private val quad = QuadBuilder(context.language, DBpediaDatasets.RevisionIds, wikiPageRevisionIdProperty, context.ontology.datatypes("xsd:integer")) _

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    val objectLink = node.root.title.pageIri
    Seq(quad(objectLink, node.revision.toString, node.sourceUri))
  }
}