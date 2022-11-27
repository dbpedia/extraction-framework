package org.dbpedia.extraction.mappings

import java.net.URL
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls



class HistoryPageExtractor(
  context : {
    def ontology: Ontology
    def language: Language
  }
)
  extends WikiPageWithRevisionsExtractor {

  //PageNodeExtractor? WikiPageExtractor ?
  private val subjectOntProperty = context.ontology.properties("dc:subject")
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val featureOntClass = context.ontology.classes("prov:Revision")
  private val wikiPageRevisionIdProperty = context.ontology.properties("wikiPageRevisionID")
  private val foafNick = context.ontology.properties("foaf:nick")

  val wikiPageLengthProperty = context.ontology.properties("wikiPageLength")
  val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  // PROPERTIES TO ADD INTO THE ONTOLOGY
  private val Entityclass = "http://www.w3.org/ns/prov#Entity"
  private val derivedFromProperty = "http://www.w3.org/ns/prov#wasDerivedFrom"
  private val quadpropderivedFromProperty = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, derivedFromProperty, null) _
  private val propQualifRevision = "http://www.w3.org/ns/prov#qualifiedRevision"
  private val quadpropQualifRevision = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propQualifRevision, null) _
  private val propCreated = "http://purl.org/dc/terms/created"
  private val quadpropCreated = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propCreated,  context.ontology.datatypes("xsd:dateTime")) _
  private val propwasRevisionOf = "http://www.w3.org/ns/prov#wasRevisionOf"
  private val quadpropwasRevisionOf = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propwasRevisionOf, null) _
  private val propCreator = "http://purl.org/dc/terms/creator"
  private val quadpropCreator = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propCreator, null) _
  private val propsciorid= "http://rdfs.org/sioc/ns#id"
  private val quadpropsciorid = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propsciorid, context.ontology.datatypes("xsd:string")) _
  private val propsciorip = "http://rdfs.org/sioc/ns#ip_address"
  private val quadpropsciorip = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propsciorip, context.ontology.datatypes("xsd:string")) _
  private val propsciornote = "http://rdfs.org/sioc/ns#note"
  private val quadpropsciocnote = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propsciornote, context.ontology.datatypes("xsd:string")) _


  override val datasets = Set(DBpediaDatasets.HistoryData) //# ToADD


  override def extract(page: WikiPageWithRevisions , subjectUri: String): Seq[Quad] = {

    println("xxxxxxxxxxxxxx HISTORY EXTRACTOR xxxxxxxxxxxxxxxxx")

    val quads = new ArrayBuffer[Quad]()
    var rev_index=0;
    quads += new Quad(context.language, DBpediaDatasets.HistoryData, page.title.pageIri, typeOntProperty, Entityclass, page.sourceIri)
    // TODO : ADD STATS TO REVISION NODE CLASS FOR
    // uniqueContributorNb
    // dbfr:revPerYear
    // dbfr:revPerMonth
    // averageSizePerYear
    // averageSizePerMonth

    page.revisions.foreach(revision => {
      quads += new Quad(context.language, DBpediaDatasets.HistoryData, page.title.pageIri, typeOntProperty, Entityclass, page.sourceIri)
      quads += quadpropQualifRevision(page.title.pageIri, revision.pageUri, page.sourceIri) // NOT 100% SUR OF IT
      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, wikiPageRevisionIdProperty,revision.id.toString, page.sourceIri, context.ontology.datatypes("xsd:integer"))
      quads += quadpropCreated(revision.pageUri,  revision.timestamp, page.sourceIri)
      quads += quadpropwasRevisionOf(revision.pageUri,  revision.parent_Uri, page.sourceIri)

      //// CREATOR
      var bn_propCreator=subjectUri+"__creator__"+rev_index
      quads += quadpropCreator(revision.pageUri, bn_propCreator, page.sourceIri)
      if(revision.contributorID != ""){
        quads += quadpropsciorid(bn_propCreator,revision.contributorID , page.sourceIri)
      }
      if (revision.contributorIP != "") {
        quads += quadpropsciorip(bn_propCreator, revision.contributorIP, page.sourceIri)
      }
      if (revision.contributorName != "") {
        quads += new Quad(context.language, DBpediaDatasets.HistoryData, bn_propCreator, foafNick, revision.contributorName, page.sourceIri)
      }
      if (revision.comment != "") {
        quads += quadpropsciocnote(revision.pageUri,revision.comment, page.sourceIri)
      }

      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, wikiPageLengthProperty, revision.text_size.toString, page.sourceIri, nonNegativeInteger)

      // TODO : CREATE OR FIND PROP FOR  :
      // revision.contributorDeleted
      // revision.minor_edit
      // revision.text_delta

      rev_index+=1
    })



    quads
  }


}
