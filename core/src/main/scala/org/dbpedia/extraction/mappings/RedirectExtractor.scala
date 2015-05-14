package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import scala.language.reflectiveCalls

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
class RedirectExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
  extends WikiPageExtractor
{
  private val language = context.language

  private val wikiPageRedirectsProperty = context.ontology.properties("wikiPageRedirects")

  override val datasets = Set(DBpediaDatasets.Redirects)

  private val namespaces = if (language == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata
    else Set(Namespace.Main, Namespace.Template, Namespace.Category)

  private val quad = QuadBuilder(language, DBpediaDatasets.Redirects, wikiPageRedirectsProperty, null) _

  override def extract(page : WikiPage, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    if (page.redirect != null && page.title.namespace == page.redirect.namespace) {
      return Seq(quad(subjectUri, language.resourceUri.append(page.redirect.decodedWithNamespace), page.sourceUri))
    }

    Seq.empty
  }
}
