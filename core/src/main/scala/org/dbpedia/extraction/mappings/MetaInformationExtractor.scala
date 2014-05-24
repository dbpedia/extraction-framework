package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import java.net.URI
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 9/13/11
 * Time: 9:03 PM
 * Extracts page's meta-information e.g. editlink, revisonlink, ....
 */

class MetaInformationExtractor( context : {
  def ontology : Ontology
  def language : Language } ) extends WikiPageExtractor
{
  /**
   * Don't access context directly in methods. Cache context.language for use inside methods so that
   * Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language

  val modificationDatePredicate = context.ontology.properties("wikiPageModified")
  val extractionDatePredicate = context.ontology.properties("wikiPageExtracted")
  val editLinkPredicate = context.ontology.properties("wikiPageEditLink")
  val revisionPredicate = context.ontology.properties("wikiPageRevisionLink")
  val historyPredicate = context.ontology.properties("wikiPageHistoryLink")
  val datetime = context.ontology.datatypes("xsd:dateTime")

  override val datasets = Set(DBpediaDatasets.RevisionMeta)

  override def extract(page : WikiPage, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    val editLink     = language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=edit"
    val revisionLink = language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&oldid=" + page.revision
    val historyLink  = language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=history"

    val quadModificationDate = new Quad(language, DBpediaDatasets.RevisionMeta, subjectUri, modificationDatePredicate,
      formatTimestamp(page.timestamp), page.sourceUri, datetime )

    val quadExtractionDate = new Quad(language, DBpediaDatasets.RevisionMeta, subjectUri, extractionDatePredicate,
      formatCurrentTimestamp, page.sourceUri, datetime )

    val quadEditlink = new Quad(language, DBpediaDatasets.RevisionMeta, subjectUri, editLinkPredicate,
      editLink, page.sourceUri, null )

    val quadRevisionlink = new Quad(language, DBpediaDatasets.RevisionMeta, subjectUri, revisionPredicate,
      revisionLink, page.sourceUri, null )

    val quadHistorylink = new Quad(language, DBpediaDatasets.RevisionMeta, subjectUri, historyPredicate,
      historyLink, page.sourceUri, null )


    Seq(quadModificationDate, quadExtractionDate, quadEditlink, quadRevisionlink, quadHistorylink)


  }
}