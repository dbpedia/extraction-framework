package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
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
    
    val quads = new ArrayBuffer[Quad]()

    quads += new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, subjectUri, isPrimaryTopicOf,  page.title.pageIri, page.sourceIri)
    quads += new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, primaryTopic, subjectUri, page.sourceIri)
    quads += new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, dcLanguage, context.language.wikiCode, page.sourceIri)
    quads += new Quad(context.language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, typeOntProperty, foafDocument.uri, page.sourceIri)

    quads
  }
}
