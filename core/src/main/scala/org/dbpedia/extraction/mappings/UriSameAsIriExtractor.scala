package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.Language
import java.net.URI

import org.dbpedia.iri.UriUtils

import scala.language.reflectiveCalls
import scala.util.{Failure, Success}

/**
 * Extracts sameAs links for resources with themselves. Only makes sense when serialization is
 * configured such that subjects are IRIs and objects are URIs (or vice versa).
 */
class UriSameAsIriExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val language = context.language

  val sameAsProperty: OntologyProperty = context.ontology.properties("owl:sameAs")
  
  val quad = QuadBuilder(context.language, DBpediaDatasets.UriSameAsIri, sameAsProperty, null) _

  override val datasets = Set(DBpediaDatasets.UriSameAsIri)

  override def extract(page: PageNode, subjectUri: String): Seq[Quad] =
  {
    // only extract triple if IRI is actually different from URI
    //
    // CompositeExtractor.extract has no try/catch around individual extractors, so throwing
    // here (as this used to) would abort every other extractor's output for the whole page
    // over a single malformed subjectUri -- skip just this page's sameAs quad instead, like
    // the rest of the codebase does for unparseable input.
    UriUtils.createURI(subjectUri) match{
      case Success(u) =>
        val encodedUri = u.toASCIIString
        if (encodedUri == subjectUri)
          Seq.empty
        else
          Seq(quad(encodedUri, subjectUri, page.sourceIri))
      case Failure(_) => Seq.empty
    }
  }
}