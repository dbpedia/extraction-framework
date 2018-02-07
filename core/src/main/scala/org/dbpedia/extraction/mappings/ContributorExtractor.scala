package org.dbpedia.extraction.mappings

import java.net.URLEncoder

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 11/29/11
 * Time: 5:49 PM
 * Extracts the information that describes the contributor (editor) of a Wikipedia page, such as his username, and his ID.
 */

@SoftwareAgentAnnotation(classOf[ContributorExtractor], AnnotationType.Extractor)
class ContributorExtractor( context : {
  def ontology : Ontology
  def language : Language } ) extends WikiPageExtractor
{

  private val language = context.language

  override val datasets = Set(DBpediaDatasets.RevisionMeta)

  private val labelProperty = context.ontology.properties("rdfs:label")
  private val stringDatatype = context.ontology.datatypes("xsd:string")
  private val integerDatatype = context.ontology.datatypes("xsd:integer")
  //Required predicates
  private val contributorPredicate = "http://dbpedia.org/meta/contributor"
  //val contributorNamePredicate = "http://dbpedia.org/property/contributorName";
  private val contributorIDPredicate = "http://dbpedia.org/meta/contributorID"

  private val qb = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.RevisionMeta)

  override def extract(node : WikiPage, subjectUri : String) : Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) 
        return Seq.empty

    if(node.contributorID <= 0) return Seq.empty

    val pageURL = language.baseUri + "/wiki/" + node.title.encoded

    //The username may contain spaces, so we should replace the spaces with underscores "_", as they ar not allowed in
    //URLs
    val contributorNameWithoutSpaces = node.contributorName.replace (" ", "_")
    val contributorURL =  "http://dbpedia.org/contributor/" + URLEncoder.encode (contributorNameWithoutSpaces,"UTF-8")

    //construct Quads
    qb.setSourceUri(node.sourceIri)
    qb.setNodeRecord(node.getNodeRecord)
    qb.setExtractor(this.softwareAgentAnnotation)

    val quadPageWithContributor = qb.clone
    quadPageWithContributor.setSubject(pageURL)
    quadPageWithContributor.setPredicate(contributorPredicate)
    quadPageWithContributor.setValue(contributorURL)

    val quadContributorName = qb.clone
    quadContributorName.setSubject(contributorURL)
    quadContributorName.setPredicate(labelProperty)
    quadContributorName.setValue(node.contributorName)
    quadContributorName.setDatatype(stringDatatype)

    val quadContributorID = qb.clone
    quadContributorID.setSubject(contributorURL)
    quadContributorID.setPredicate(contributorIDPredicate)
    quadContributorID.setValue(node.contributorID.toString)
    quadContributorName.setDatatype(integerDatatype)

    Seq(quadPageWithContributor.getQuad, quadContributorName.getQuad, quadContributorID.getQuad)
  }
}
