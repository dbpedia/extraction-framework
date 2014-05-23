package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts links to corresponding Articles in Wikipedia.
 */
class ArticlePageExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  /**
   * Don't access context directly in methods. Cache context.language for use inside methods so that
   * Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language

  // We used foaf:page here, but foaf:isPrimaryTopicOf is probably better.
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val foafDocument = context.ontology.classes("foaf:Document")

  override val datasets = Set(DBpediaDatasets.LinksToWikipediaArticle)

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    val quads = new ArrayBuffer[Quad]()

    quads += new Quad(language, DBpediaDatasets.LinksToWikipediaArticle, subjectUri, isPrimaryTopicOf,  page.title.pageIri, page.sourceUri)
    quads += new Quad(language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, primaryTopic, subjectUri, page.sourceUri)
    quads += new Quad(language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, dcLanguage, language.wikiCode, page.sourceUri)
    quads += new Quad(language, DBpediaDatasets.LinksToWikipediaArticle, page.title.pageIri, typeOntProperty, foafDocument.uri, page.sourceUri)

    quads
  }
}
