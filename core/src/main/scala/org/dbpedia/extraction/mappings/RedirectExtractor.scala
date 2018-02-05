package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

import scala.language.reflectiveCalls

/**
 * Extracts redirect links between Articles in Wikipedia.
 */
@SoftwareAgentAnnotation(classOf[RedirectExtractor], AnnotationType.Extractor)
class RedirectExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
  extends PageNodeExtractor
{
  private val language = context.language

  private val wikiPageRedirectsProperty = context.ontology.properties("wikiPageRedirects")

  override val datasets = Set(DBpediaDatasets.Redirects)

  private val namespaces = if (language == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata
    else Set(Namespace.Main, Namespace.Template, Namespace.Category)

  private val qb = QuadBuilder(language, DBpediaDatasets.Redirects, wikiPageRedirectsProperty, null)

  override def extract(page : PageNode, subjectUri : String): Seq[Quad] = {
    if (page.isRedirect && page.title.namespace == page.redirect.namespace) {
      qb.setNodeRecord(page.getNodeRecord)
      qb.setExtractor(this.softwareAgentAnnotation)
      qb.setSubject(subjectUri)
      qb.setValue(language.resourceUri.append(page.redirect.decodedWithNamespace))
      qb.setSourceUri(page.sourceIri)
      Seq(qb.getQuad)
    }
    else
      Seq.empty
  }
}
