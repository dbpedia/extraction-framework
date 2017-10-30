package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 9/13/11
 * Time: 9:03 PM
 * Extracts page's meta-information e.g. editlink, revisonlink, ....
 */

@SoftwareAgentAnnotation(classOf[MetaInformationExtractor], AnnotationType.Extractor)
class MetaInformationExtractor( context : {
  def ontology : Ontology
  def language : Language } ) extends WikiPageExtractor
{
  val modificationDatePredicate = context.ontology.properties("wikiPageModified")
  val extractionDatePredicate = context.ontology.properties("wikiPageExtracted")
  val editLinkPredicate = context.ontology.properties("wikiPageEditLink")
  val revisionPredicate = context.ontology.properties("wikiPageRevisionLink")
  val historyPredicate = context.ontology.properties("wikiPageHistoryLink")
  val datetime = context.ontology.datatypes("xsd:dateTime")

  override val datasets = Set(DBpediaDatasets.RevisionMeta)

  override def extract(page : WikiPage, subjectUri : String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    val editLink     = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=edit"
    val revisionLink = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&oldid=" + page.revision
    val historyLink  = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=history"

    val quadModificationDate = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, modificationDatePredicate,
      formatTimestamp(page.timestamp), page.sourceIri, datetime )

    val quadExtractionDate = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, extractionDatePredicate,
      formatCurrentTimestamp, page.sourceIri, datetime )

    val quadEditlink = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, editLinkPredicate,
      editLink, page.sourceIri, null )

    val quadRevisionlink = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, revisionPredicate,
      revisionLink, page.sourceIri, null )

    val quadHistorylink = new Quad(context.language, DBpediaDatasets.RevisionMeta, subjectUri, historyPredicate,
      historyLink, page.sourceIri, null )


    Seq(quadModificationDate, quadExtractionDate, quadEditlink, quadRevisionlink, quadHistorylink)


  }
}