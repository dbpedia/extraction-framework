package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts links to corresponding Articles in Wikipedia.
 */
@SoftwareAgentAnnotation(classOf[ArticlePageExtractor], AnnotationType.Extractor)
class ArticlePageExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  // We used foaf:page here, but foaf:isPrimaryTopicOf is probably better.
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val foafDocument = context.ontology.classes("foaf:Document")

  override val datasets = Set(DBpediaDatasets.LinksToWikipediaArticle)

  override def extract(page : PageNode, subjectUri : String): Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) 
        return Seq.empty


    val qb = new QuadBuilder(None, None, None, Some(page.sourceIri), context.language, None, Some(DBpediaDatasets.LinksToWikipediaArticle), None)
    qb.setNodeRecord(page.getNodeRecord)
    qb.setExtractor(this.softwareAgentAnnotation)

    val quads = new ArrayBuffer[Quad]()

    qb.setTriple(subjectUri, isPrimaryTopicOf.uri, page.title.pageIri)
    quads += qb.getQuad
    qb.setTriple(page.title.pageIri, primaryTopic.uri, subjectUri)
    quads += qb.getQuad
    qb.setTriple(page.title.pageIri, dcLanguage.uri, context.language.wikiCode)
    quads += qb.getQuad
    qb.setTriple(page.title.pageIri, typeOntProperty.uri, foafDocument.uri)
    quads += qb.getQuad

    quads
  }
}
