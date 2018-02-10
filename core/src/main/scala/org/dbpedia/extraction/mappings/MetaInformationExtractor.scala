package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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

  private val qb = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.RevisionMeta)

  override def extract(page : WikiPage, subjectUri : String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    qb.setSubject(subjectUri)
    qb.setExtractor(this.softwareAgentAnnotation)
    qb.setNodeRecord(page.getNodeRecord)
    qb.setSourceUri(page.sourceIri)

    val editLink     = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=edit"
    val revisionLink = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&oldid=" + page.revision
    val historyLink  = context.language.baseUri + "/w/index.php?title=" + page.title.encodedWithNamespace + "&action=history"

    val qb1 = qb.clone
    qb1.setPredicate(modificationDatePredicate)
    qb1.setValue(formatTimestamp(page.timestamp))
    qb1.setDatatype(datetime)

    val qb2 = qb.clone
    qb2.setPredicate(extractionDatePredicate)
    qb2.setValue(formatCurrentTimestamp)
    qb2.setDatatype(datetime)

    val qb3 = qb.clone
    qb3.setPredicate(editLinkPredicate)
    qb3.setValue(editLink)

    val qb4 = qb.clone
    qb4.setPredicate(revisionPredicate)
    qb4.setValue(revisionLink)

    val qb5 = qb.clone
    qb5.setPredicate(historyPredicate)
    qb5.setValue(historyLink)

    Seq(qb1.getQuad, qb2.getQuad, qb3.getQuad, qb4.getQuad, qb5.getQuad)


  }
}