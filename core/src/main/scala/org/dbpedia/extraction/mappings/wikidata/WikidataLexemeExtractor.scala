package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.mappings.wikidata.WikidataMappingConfig
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
  * Lexeme extractor extracts data in the form of
  * getLexeme method:
<http://lex.dbpedia.org/wikidata/L536> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L536> .
<http://lex.dbpedia.org/wikidata/L536> <http://www.w3.org/ns/lemon/ontolex#lexicalForm> <http://lex.dbpedia.org/wikidata/L536-F1> .
<http://lex.dbpedia.org/wikidata/L536> <http://www.w3.org/ns/lemon/ontolex#sense> <http://lex.dbpedia.org/wikidata/L536-S1> .
  * getLemma method returns data in form of:
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/lexeme> <http://lex.dbpedia.org/wikidata/L536> .
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/lexicalcategory> <http://www.wikidata.org/entity/Q1084> .
<http://lex.dbpedia.org/resource/book> <http://dbpedia.org/ontology/language> <http://www.wikidata.org/entity/Q1860> .
  * one statement from lexeme:
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/P5402> <http://www.wikidata.org/entity/L16168> .
  * getForms method returns data in form of:
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/form> <http://lex.dbpedia.org/wikidata/L536-F1> .
<http://lex.dbpedia.org/wikidata/L536-F1> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L536-F1> .
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/grammaticalFeature> <http://www.wikidata.org/entity/Q110786> .
  * one statement from form:
<http://lex.dbpedia.org/resource/book> <http://lex.dbpedia.org/property/P898> "/bʊk/" .
  * getSenses returns data in form of:
<http://lex.dbpedia.org/resource/document> <http://lex.dbpedia.org/property/lexicalSense> <http://lex.dbpedia.org/wikidata/L536-S1> .
<http://lex.dbpedia.org/wikidata/L536-S1> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L536-S1> .
  * one statement from sense:
<http://lex.dbpedia.org/resource/document> <http://lex.dbpedia.org/property/P18> <http://commons.wikimedia.org/wiki/File:Books_HD_(8314929977).jpg> .

  */
class WikidataLexemeExtractor(
                               context: {
                                 def ontology: Ontology
                                 def language: Language
                               }
                             )
  extends JsonNodeExtractor {

  private val lexemeDbpedia: String = "http://lex.dbpedia.org/"

  private val subjectResource: String = "http://lex.dbpedia.org/resource/"
  private val subjectWikidata: String = "http://lex.dbpedia.org/wikidata/"
  private val property: String = "http://lex.dbpedia.org/property/"
  private val propertyLemma: String = "http://lex.dbpedia.org/property/lemma"
  private val propertyForm: String = "http://lex.dbpedia.org/property/form"
  private val propertySense: String = "http://lex.dbpedia.org/property/lexicalSense"
  private val propertyGrammaticalFeature: String = "http://lex.dbpedia.org/property/grammaticalFeature"
  private val propertyLexicalCategory: String = "http://lex.dbpedia.org/property/lexicalcategory"
  private val propertyOntolexSense: String = "http://www.w3.org/ns/lemon/ontolex#sense"
  private val propertyOntolexLexicalForm: String = "http://www.w3.org/ns/lemon/ontolex#lexicalForm"
  private val propertyLanguage: String = "http://dbpedia.org/ontology/language"
  private val sameAsProperty = context.ontology.properties("owl:sameAs")
  private val rdfType = context.ontology.properties("rdf:type")

  private val wikibaseLexeme = "http://wikiba.se/ontology#Lexeme"
  private val wikibaseSense = "http://wikiba.se/ontology#Sense"
  private val wikibaseForm = "http://wikiba.se/ontology#Form"

  private val lexicalEntry = "http://www.w3.org/ns/lemon/ontolex#LexicalEntry"
  private val formOntolex = "http://www.w3.org/ns/lemon/ontolex#Form"
  private val senseOntolex = "http://www.w3.org/ns/lemon/ontolex#LexicalSense"

  //TODO: add some more formats if exists
  private val listOfWikiCommonsFileTypes = Set(".*\\.jpg\\b".r, ".*\\.svg\\b".r,".png\\b".r, ".*\\.gif\\b".r,
    ".*\\.webp\\b".r,".*\\.tiff\\b".r, ".xcf\\b".r, ".*\\.oga\\b".r, ".*\\.wav\\b".r, ".*\\.ogg\\b".r,".*\\.ogx\\b".r,
    ".*\\.ogv\\b".r, ".*\\.mp3\\b".r,".*\\.opus\\b".r, ".flac\\b".r, ".webm\\b".r, ".*\\.pdf\\b".r,
    ".*\\.mid\\b".r,".*\\.djvu\\b".r, ".*\\.map\\b".r, ".*\\.tab\\b".r, ".*\\.stl\\b".r)

  override val datasets = Set(DBpediaDatasets.WikidataLexeme)


  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val subject = WikidataUtil.getWikidataNamespace(subjectUri).replace("Lexeme:", "")
    quads ++= getLexemes(page, subject)
    quads ++= getLemmas(page, subject)
    quads ++= getForms(page, subject)
    quads ++= getSenses(page, subject)

    quads
  }

  private def getLexemes(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      page.getEntityId match {
        case value: Value => {
          val lexeme = subjectWikidata + value.getId
          quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, sameAsProperty, subjectUri, document.wikiPage.sourceIri, null)
          quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, rdfType, lexicalEntry, document.wikiPage.sourceIri, null)
          quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, rdfType, wikibaseLexeme, document.wikiPage.sourceIri, null)

          for (form <- page.getForms) {
            val lexemeForm = subjectWikidata + form.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, propertyOntolexLexicalForm, lexemeForm, document.wikiPage.sourceIri, null)
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeForm, rdfType, formOntolex,  document.wikiPage.sourceIri, null)
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeForm, rdfType, wikibaseForm,  document.wikiPage.sourceIri, null)
          }

          for (sense <- page.getSenses) {
            val lexemeSense = subjectWikidata + sense.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, propertyOntolexSense, lexemeSense, document.wikiPage.sourceIri, null)
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeSense, rdfType, senseOntolex, document.wikiPage.sourceIri, null)
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeSense, rdfType, wikibaseSense, document.wikiPage.sourceIri, null)
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
            val lemmaIri = WikidataUtil.replaceSpaceWithUnderscore(subjectResource + lemma.getText)
            val subject = subjectWikidata + page.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLemma, subject,
              document.wikiPage.sourceIri, null)

            for (statementGroup <- page.getStatementGroups) {
              quads ++= getStatements(lemmaIri, statementGroup, document)
            }

            page.getLexicalCategory match {
              case lexicalCategoryValue: Value => {
                if (WikidataMappingConfig.lexicalCategoryMap.contains(lexicalCategoryValue.getId)){
                  val objectValue = lexemeDbpedia + WikidataMappingConfig.lexicalCategoryMap(lexicalCategoryValue.getId)
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLexicalCategory, objectValue,
                    document.wikiPage.sourceIri, null)
                }
                else {
                  val objectValue = WikidataUtil.getWikidataNamespace(lexicalCategoryValue.getIri)
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLexicalCategory, objectValue,
                    document.wikiPage.sourceIri, null)
                }
              }
              case _ =>
            }

            page.getLanguage match {
              case languageValue: Value => {
                if (WikidataMappingConfig.languagesMap.contains(languageValue.getId)){
                  val language = lexemeDbpedia + WikidataMappingConfig.languagesMap(languageValue.getId)
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLanguage,language,document.wikiPage.sourceIri, null)
                }
              }
              case _ =>
            }

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
        val lexemeForm = subjectWikidata + form.getEntityId.getId
        for ((_, representation) <- form.getRepresentations) {
          representation match {
            case value: Value => {
              val formWikidata = WikidataUtil.getValue(form.getEntityId)
              val formRepresentation = WikidataUtil.replaceSpaceWithUnderscore(subjectResource + value.getText)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, formRepresentation, propertyForm, lexemeForm, document.wikiPage.sourceIri, null)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeForm, sameAsProperty, formWikidata, document.wikiPage.sourceIri,null)

              for (grammaticalFeature <- form.getGrammaticalFeatures) {
                val grammaticalFeatureObject = WikidataUtil.getWikidataNamespace(grammaticalFeature.getIri)

                quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, formRepresentation, propertyGrammaticalFeature, grammaticalFeatureObject, document.wikiPage.sourceIri,null)
              }

              for (statementGroup <- form.getStatementGroups) {
                quads ++= getStatements(formRepresentation,statementGroup,document)
              }
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
              val senseWikidata = subjectWikidata + sense.getEntityId.getId
              val subjectIri = (subjectResource + value.getText).replace(" ", "_")
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, propertySense, senseWikidata,
                document.wikiPage.sourceIri, null)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, senseWikidata, sameAsProperty, senseIri,
                document.wikiPage.sourceIri, null)

              for (statementGroup <- sense.getStatementGroups) {
                quads ++= getStatements(subjectIri, statementGroup, document)
              }
            }
            case _ =>
          }
        }
      }
    }
    quads
  }

  private def getStatements(subject: String, statementGroup: StatementGroup, document: JsonNode ): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    statementGroup.foreach {
      statement => {
        val claim = statement.getClaim
        val propertyStatement = property + WikidataUtil.getId(claim.getMainSnak.getPropertyId)

        claim.getMainSnak match {
          case mainSnak: ValueSnak => {
            val v = mainSnak.getValue
            v match {
              case entity: EntityIdValue => {
                val objectValue = WikidataUtil.getWikidataNamespace(WikidataUtil.getUrl(entity))
                quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subject, propertyStatement, objectValue,document.wikiPage.sourceIri,  null)
              }
              case text: Value => {
                if (listOfWikiCommonsFileTypes.exists(regex => regex.findFirstIn(text.toString).isDefined)) {
                  val statementValue = WikidataUtil.getWikiCommonsUrl(WikidataUtil.getValue(text))
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subject, propertyStatement, statementValue, document.wikiPage.sourceIri, null)
                }
                else {
                  val statementValue = WikidataUtil.replaceSpaceWithUnderscore(WikidataUtil.getValue(text))
                  val datatype = if (WikidataUtil.getDatatype(v) != null) context.ontology.datatypes(WikidataUtil.getDatatype(v)) else null
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subject, propertyStatement, statementValue, document.wikiPage.sourceIri, datatype)
                }
              }
              case _ =>
            }
          }
          case _ =>
        }
      }
    }
    quads
  }
}
