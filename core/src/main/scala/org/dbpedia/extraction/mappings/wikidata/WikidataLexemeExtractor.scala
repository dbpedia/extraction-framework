package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.mappings.JsonNodeExtractor
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
  * Lexeme extractor extracts data on the form of
  * <http://lex.dbpedia.org/wikidata/L221524> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L221524> .
  * <http://lex.dbpedia.org/resource/fukssvans> <http://lex.dbpedia.org/property/lexeme> <http://lex.dbpedia.org/wikidata/L221524> .
  * <http://lex.dbpedia.org/resource/fukssvans> <http://lex.dbpedia.org/property/form> <http://www.wikidata.org/entity/L221524-F1> .
  *
  */
class WikidataLexemeExtractor(
                               context: {
                                 def ontology: Ontology
                                 def language: Language
                               }
                             )
  extends JsonNodeExtractor {

  private val subjectResource: String = "http://lex.dbpedia.org/resource/"
  private val subjectWikidata: String = "http://lex.dbpedia.org/wikidata/"
  private val lexemeProperty: String = "http://lex.dbpedia.org/property/lexeme"
  private val formProperty: String = "http://lex.dbpedia.org/property/form"
  private val senseProperty: String = "http://lex.dbpedia.org/property/lexicalSense"
  private val property: String = "http://lex.dbpedia.org/property/"
  private val ontolexProperty: String = "http://www.w3.org/ns/lemon/ontolex#lexicalForm"
  private val sameAsProperty = context.ontology.properties("owl:sameAs")
  override val datasets = Set(DBpediaDatasets.WikidataLexeme)


  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val subject = WikidataUtil.getWikidataNamespace(subjectUri).replace("Lexeme:", "")
    quads ++= getLexeme(page, subject)
    quads ++= getLemmas(page, subject)
    quads ++= getForms(page, subject)
    quads ++= getSenses(page, subject)

    quads
  }

  private def getLexeme(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      page.getEntityId match {
        case value: Value => {
          val lexeme = subjectWikidata + value.getId
          quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, sameAsProperty, subjectUri, document.wikiPage.sourceIri, null)
          for (form <- page.getForms) {
            val lexemeForm = subjectWikidata + form.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, ontolexProperty, lexemeForm, document.wikiPage.sourceIri, null)
          }
        }
        case _ =>
      }
    }
    quads
  }

  private def getLemmas(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      for ((_, value) <- page.getLemmas) {
        value match {
          case lemma: Value => {
            val lemmaIri = (subjectResource + lemma.getText).replace(" ", "_")
            val subject = subjectWikidata + page.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, lexemeProperty, subject,
              document.wikiPage.sourceIri, null)
          }
          case _ =>
        }
      }
    }
    quads
  }

  private def getForms(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      for (form <- page.getForms) {
        for ((_, representation) <- form.getRepresentations) {
          representation match {
            case value: Value => {
              val formIri = WikidataUtil.getValue(form.getEntityId)
              val subjectIri = (subjectResource + value.getText).replace(" ", "_")
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, formProperty, formIri,
                document.wikiPage.sourceIri, null)
            }
            case _ =>
          }
        }

      }

    }

    quads
  }

  private def getSenses(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      for (sense <- page.getSenses) {
        for ((_, lexicalSense) <- sense.getGlosses) {

          lexicalSense match {
            case value: Value => {

              val senseIri = WikidataUtil.getValue(sense.getEntityId)
              val subjectIri = (subjectResource + value.getText).replace(" ", "_")
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, senseProperty, senseIri,
                document.wikiPage.sourceIri, null)

              for (statementGroup <- sense.getStatementGroups) {
                statementGroup.foreach {
                  statement => {
                    val claim = statement.getClaim
                    val lexeme = property + WikidataUtil.getId(claim.getMainSnak.getPropertyId)

                    claim.getMainSnak match {
                      case mainSnak: ValueSnak => {
                        val v = mainSnak.getValue
                        v match {
                          case entity: EntityIdValue => {
                            val objectValue = WikidataUtil.getWikidataNamespace(WikidataUtil.getUrl(entity))

                            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, lexeme, objectValue, document.wikiPage.sourceIri, null)

                          }
                          case _ =>
                        }

                      }
                      case _ =>
                    }
                  }
                }
              }
            }
            case _ =>
          }

        }

      }
    }
    quads
  }

}