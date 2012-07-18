package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.QuadBuilder

/**
 * Extracts sameAs links for resources with themselves. Only makes sense when serialization is
 * configured such that subjects are IRIs and objects are URIs (or vice versa).
 */
class IriSameAsUriExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends Extractor
{
  private val language = context.language

  val sameAsProperty = context.ontology.properties("owl:sameAs")
  
  val quad = QuadBuilder(context.language, DBpediaDatasets.IriSameAsUri, sameAsProperty, null) _

  override val datasets = Set(DBpediaDatasets.IriSameAsUri)

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    // TODO: only extract triple if IRI is different from URI
    Seq(quad(subjectUri, subjectUri, page.sourceUri))
  }
}