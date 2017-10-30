package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Created by ali on 10/26/14.
 * Raw wikidata statements extracted on the form of
 * wd:Q64 wikidata:P6 wd:Q8863.
 *
 * In order to extract n-ary relation statements are reified.
 * For reification unique statement URIs is created.
 * Mapped statements reified on the form of
 * wd:Q64_P6_Q8863 rdf:type rdf:Statement.
 * wd:Q64_P6_Q8863 rdf:subject wd:Q64 .
 * wd:Q64_P6_Q8863 rdf:predicate wikidata:P6.
 * wd:Q64_P6_Q8863 rdf:object wd:Q8863.
 *
 * Qualifiers use same statement URIs and extracted on the form of
 * wd:Q64_P6_Q8863 wikidata:P580 "2001-6-16"^^xsd:date.
 * wd:Q64_P6_Q8863 wikidata:P582 "2014-12-11"^^xsd:date.
 *
 */
@SoftwareAgentAnnotation(classOf[WikidataRawExtractor], AnnotationType.Extractor)
class WikidataRawExtractor(
                            context: {
                              def ontology: Ontology
                              def language: Language
                            }
                            )
  extends JsonNodeExtractor {

  private val rdfType = context.ontology.properties("rdf:type")
  private val rdfStatement = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement"
  private val rdfSubject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject"
  private val rdfPredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate"
  private val rdfObject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#object"

  override val datasets = Set(DBpediaDatasets.WikidataRaw, DBpediaDatasets.WikidataRawReified, DBpediaDatasets.WikidataRawReifiedQualifiers)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      for ((statementGroup) <- page.wikiDataDocument.getStatementGroups) {
        statementGroup.getStatements.foreach {
          statement => {
            val claim = statement.getClaim
            val propertyId = claim.getMainSnak.getPropertyId.getId
            val propertyIri = claim.getMainSnak.getPropertyId.getIri
            val property = WikidataUtil.getWikidataNamespace(propertyIri)

            claim.getMainSnak match {
              case mainSnak: ValueSnak => {
                val value = mainSnak.getValue

                val datatypeiri = WikidataUtil.getDatatype(value)
                val datatype = if (datatypeiri != null) context.ontology.datatypes(datatypeiri) else null

                //Wikidata raw extractor without reification
                val valuei = WikidataUtil.getValue(value)
                quads += new Quad(context.language, DBpediaDatasets.WikidataRaw, subjectUri, property, valuei, page.wikiPage.sourceIri, datatype)

                //unique statementUri created for reification. Same statementUri is used for reification mapping
                val statementUri = WikidataUtil.getStatementUri(subjectUri, propertyId, value)

                //Wikidata raw extractor with reification
                quads += new Quad(context.language, DBpediaDatasets.WikidataRawReified, statementUri, rdfType, rdfStatement, page.wikiPage.sourceIri)
                quads += new Quad(context.language, DBpediaDatasets.WikidataRawReified, statementUri, rdfSubject, subjectUri, page.wikiPage.sourceIri, null)
                quads += new Quad(context.language, DBpediaDatasets.WikidataRawReified, statementUri, rdfPredicate, property, page.wikiPage.sourceIri, null)
                quads += new Quad(context.language, DBpediaDatasets.WikidataRawReified, statementUri, rdfObject, valuei, page.wikiPage.sourceIri, datatype)
                quads ++= getQualifiersQuad(page, statementUri, claim)
              }
              case _ =>

            }
          }
        }
      }
    }
    quads
  }

  private def getQualifiersQuad(page: JsonNode, statementUri:String, claim:Claim):Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    claim.getQualifiers.foreach {
      qualifier => {

        for (qual <- qualifier) {
          qual match {
            case valueSnak: ValueSnak => {
              val qualifierPropertyIri = valueSnak.getPropertyId.getIri
              val qualifierProperty = WikidataUtil.getWikidataNamespace(qualifierPropertyIri)
              val qualifierValue = valueSnak.getValue
              val qDatatype = if (WikidataUtil.getDatatype(qualifierValue) != null) context.ontology.datatypes(WikidataUtil.getDatatype(qualifierValue)) else null

              //Wikidata raw qualifiers without r2r mapping
              quads += new Quad(context.language, DBpediaDatasets.WikidataRawReifiedQualifiers,
                statementUri, qualifierProperty, WikidataUtil.getValue(qualifierValue), page.wikiPage.sourceIri, qDatatype)
            }
            case _ =>
          }
        }
      }
    }
    quads
  }
}
