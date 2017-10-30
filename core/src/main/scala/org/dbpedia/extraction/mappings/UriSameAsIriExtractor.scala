package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.Language
import java.net.URI

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.iri.UriUtils

import scala.language.reflectiveCalls
import scala.util.{Failure, Success}

/**
 * Extracts sameAs links for resources with themselves. Only makes sense when serialization is
 * configured such that subjects are IRIs and objects are URIs (or vice versa).
 */
@SoftwareAgentAnnotation(classOf[UriSameAsIriExtractor], AnnotationType.Extractor)
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
    val encodedUri = UriUtils.createURI(subjectUri) match{
      case Success(u) => u.toASCIIString
      case Failure(f) => throw f
    }
    if (encodedUri == subjectUri)
      Seq.empty
    else
      Seq(quad(encodedUri, subjectUri, page.sourceIri))
  }
}