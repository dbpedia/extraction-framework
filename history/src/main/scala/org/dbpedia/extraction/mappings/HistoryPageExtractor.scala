package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
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
  // PROP FROM ONTOLOGY
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val wikiPageRevisionIdProperty = context.ontology.properties("wikiPageRevisionID")
  private val foafNick = context.ontology.properties("foaf:nick")
  private val minorRevProperty = context.ontology.properties("isMinorRevision")
  private val wikiPageLengthDeltaProperty = context.ontology.properties("wikiPageLengthDelta")
  private val wikiPageLengthProperty = context.ontology.properties("wikiPageLength")
  private val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  // PROPERTIES NOT IN THE ONTOLOGY
  private val entityClass = "http://www.w3.org/ns/prov#Entity"
  private val revisionClass = "http://www.w3.org/ns/prov#Revision"
  private val propQualifRevision = "http://www.w3.org/ns/prov#qualifiedRevision"
  private val quadPropQualifRevision = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propQualifRevision, null) _
  private val propCreated = "http://purl.org/dc/terms/created"
  private val quadPropCreated = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propCreated,  context.ontology.datatypes("xsd:dateTime")) _
  private val propWasRevisionOf = "http://www.w3.org/ns/prov#wasRevisionOf"
  private val quadPropWasRevisionOf = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propWasRevisionOf, null) _
  private val propCreator = "http://purl.org/dc/terms/creator"
  private val quadPropCreator = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propCreator, null) _
  private val propSiocId= "http://rdfs.org/sioc/ns#id"
  private val quadPropSiocId = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propSiocId, context.ontology.datatypes("xsd:string")) _
  private val propPropSiocIp = "http://rdfs.org/sioc/ns#ip_address"
  private val quadPropPropSiocIp = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propPropSiocIp, context.ontology.datatypes("xsd:string")) _
  private val propSiocNote = "http://rdfs.org/sioc/ns#note"
  private val quadPropSiocNote = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryData, propSiocNote, context.ontology.datatypes("xsd:string")) _

  override val datasets = Set(DBpediaDatasets.HistoryData)


  override def extract(page: WikiPageWithRevisions , subjectUri: String): Seq[Quad] = {


    val quads = new ArrayBuffer[Quad]()
    var rev_index=0;
    quads += new Quad(context.language, DBpediaDatasets.HistoryData, page.title.pageIri, typeOntProperty, entityClass, page.sourceIri)
    //// CONTENT OF HISTORY
    page.revisions.foreach(revision => {
      quads += new Quad(context.language, DBpediaDatasets.HistoryData,revision.pageUri, typeOntProperty, revisionClass, page.sourceIri)
      quads += quadPropQualifRevision(page.title.pageIri, revision.pageUri, page.sourceIri) // NOT 100% SUR OF IT
      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, wikiPageRevisionIdProperty,revision.id.toString, page.sourceIri, context.ontology.datatypes("xsd:integer"))
      quads += quadPropCreated(revision.pageUri,  revision.timestamp, page.sourceIri)
      quads += quadPropWasRevisionOf(revision.pageUri,  revision.parent_Uri, page.sourceIri)

      //// CREATOR ID based on getUserIdAll function that give unique ID based on what data is available about the user
      val bn_propCreator=subjectUri+"__creator__"+revision.getUserIDAlt
      quads += quadPropCreator(revision.pageUri, bn_propCreator, page.sourceIri)
      if(revision.contributorID != ""){
        quads += quadPropSiocId(bn_propCreator,revision.contributorID , page.sourceIri)
      }
      if (revision.contributorIP != "") {
        quads += quadPropPropSiocIp(bn_propCreator, revision.contributorIP, page.sourceIri)
      }
      if (revision.contributorName != "") {
        quads += new Quad(context.language, DBpediaDatasets.HistoryData, bn_propCreator, foafNick, revision.contributorName, page.sourceIri)
      }
      if (revision.comment != "") {
        quads += quadPropSiocNote(revision.pageUri,revision.comment, page.sourceIri)
      }

      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, wikiPageLengthProperty, revision.text_size.toString, page.sourceIri, nonNegativeInteger)
      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, wikiPageLengthDeltaProperty, revision.text_delta.toString, page.sourceIri, context.ontology.datatypes("xsd:integer"))
      quads += new Quad(context.language, DBpediaDatasets.HistoryData, revision.pageUri, minorRevProperty, revision.minor_edit.toString, page.sourceIri, context.ontology.datatypes("xsd:boolean"))

      // TODO : CREATE OR FIND PROP FOR  :
      // revision.contributorDeleted ov:DeletedEntry ?
      rev_index+=1
    })



    quads
  }


}
