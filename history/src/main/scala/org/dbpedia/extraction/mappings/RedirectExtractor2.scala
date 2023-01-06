package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
class RedirectExtractor2(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
  extends WikiPageWithRevisionsExtractor
{
  private val language = context.language

  private val wikiPageRedirectsProperty = context.ontology.properties("wikiPageRedirects")

  override val datasets = Set(DBpediaDatasets.Redirects)

  private val namespaces = if (language == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata
    else Set(Namespace.Main, Namespace.Template, Namespace.Category)

  private val quad = QuadBuilder(language, DBpediaDatasets.Redirects, wikiPageRedirectsProperty, null) _

  override def extract(page : WikiPageWithRevisions, subjectUri : String): Seq[Quad] = {
    if (page.isRedirect && page.title.namespace == page.redirect.namespace) {
      return Seq(quad(subjectUri, language.resourceUri.append(page.redirect.decodedWithNamespace), page.sourceIri))
    }

    Seq.empty
  }
}
