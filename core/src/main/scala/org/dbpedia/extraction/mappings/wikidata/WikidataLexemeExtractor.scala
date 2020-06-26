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
  * <http://lex.dbpedia.org/wikidata/L222072> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L222072> .
  * <http://lex.dbpedia.org/wikidata/L222072> <http://www.w3.org/ns/lemon/ontolex#lexicalForm> <http://lex.dbpedia.org/wikidata/L222072-F1> .
  * <http://lex.dbpedia.org/resource/cykelsadel> <http://lex.dbpedia.org/property/lexeme> <http://lex.dbpedia.org/wikidata/L222072> .
  * <http://lex.dbpedia.org/resource/cykelsadel> <http://lex.dbpedia.org/property/form> <http://lex.dbpedia.org/wikidata/L222072-F1> .
  * <http://lex.dbpedia.org/wikidata/L222072-F1> <http://www.w3.org/2002/07/owl#sameAs> <http://www.wikidata.org/entity/L222072-F1> .
  * <http://lex.dbpedia.org/resource/sæde_på_en_cykel> <http://lex.dbpedia.org/property/lexicalSense> <http://www.wikidata.org/entity/L222072-S1> .
  * <http://lex.dbpedia.org/resource/sæde_på_en_cykel> <http://lex.dbpedia.org/property/P5137> <http://www.wikidata.org/entity/Q1076532> .
  * <http://lex.dbpedia.org/resource/sæde_på_en_cykel> <http://lex.dbpedia.org/property/P18> <http://commons.wikimedia.org/wiki/File:Bicycle_saddle.jpg> .
  *
  */
class WikidataLexemeExtractor(
                               context: {
                                 def ontology: Ontology
                                 def language: Language
                               }
                             )
  extends JsonNodeExtractor {

  private val subjectLexeme: String = "http://lex.dbpedia.org/resource/"
  private val subjectWikidata: String = "http://lex.dbpedia.org/wikidata/"
  private val property: String = "http://lex.dbpedia.org/property/"
  private val propertyLexeme: String = "http://lex.dbpedia.org/property/lexeme"
  private val propertyForm: String = "http://lex.dbpedia.org/property/form"
  private val propertySense: String = "http://lex.dbpedia.org/property/lexicalSense"
  private val propertyGrammaticalFeature: String = "http://lex.dbpedia.org/property/grammaticalFeature"
  private val propertyLexicalCategory: String = "http://lex.dbpedia.org/property/lexicalcategory"
  private val propertyOntolexSense: String = "http://www.w3.org/ns/lemon/ontolex#sense"
  private val propertyOntolexForm: String = "http://www.w3.org/ns/lemon/ontolex#lexicalForm"
  private val sameAsProperty = context.ontology.properties("owl:sameAs")

  //add some more formats if exists
  private val listOfWikiCommonsFormats = Set(".jpg".r,".svg".r,".png".r, ".gif".r,".webp".r,".tiff".r, ".xcf".r, ".oga".r, ".wav".r, ".ogg".r,".ogx".r,".ogv".r, ".mp3".r,".opus".r, ".flac".r, ".webm".r, ".pdf".r,".mid".r,".djvu".r, ".map".r, ".tab".r, ".stl".r)

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
          // extracting forms
          for (form <- page.getForms) {
            val lexemeForm = subjectWikidata + form.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, propertyOntolexForm, lexemeForm, document.wikiPage.sourceIri, null)
          }
          // extracting senses
          for (sense <- page.getSenses) {
            val lexemeSense = subjectWikidata + sense.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexeme, propertyOntolexSense, lexemeSense, document.wikiPage.sourceIri, null)
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
            val lemmaIri = WikidataUtil.replaceSpace(subjectLexeme + lemma.getText)
            val subject = subjectWikidata + page.getEntityId.getId
            quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLexeme, subject,
              document.wikiPage.sourceIri, null)

            page.getLexicalCategory match {
              case value: Value => {
                val objectValue = WikidataUtil.getWikidataNamespace(value.getIri)

                //getting lexical category
                quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lemmaIri, propertyLexicalCategory, objectValue,
                  document.wikiPage.sourceIri, null)


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
              val subjectIri = WikidataUtil.replaceSpace(subjectLexeme + value.getText)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, propertyForm, lexemeForm, document.wikiPage.sourceIri, null)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeForm, sameAsProperty, formWikidata, document.wikiPage.sourceIri,null)
              for (grammaticalFeature <- form.getGrammaticalFeatures) {
                val grammaticalFeatureObject = WikidataUtil.getWikidataNamespace(grammaticalFeature.getIri)

                quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, lexemeForm, propertyGrammaticalFeature, grammaticalFeatureObject, document.wikiPage.sourceIri,null)
              }
              for (statementGroup <- form.getStatementGroups) {
                quads ++= getStatements(subjectIri,statementGroup,document)
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
              val subjectIri = (subjectLexeme + value.getText).replace(" ", "_")
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subjectIri, propertySense, senseIri,
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

  private def getStatements(subject: String, statementGroup: StatementGroup, document: JsonNode ): Seq[Quad]={
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
                if (listOfWikiCommonsFormats.exists(regex => regex.findFirstIn(text.toString).isDefined)){
                  val objectValue = WikidataUtil.getWikiCommonsUrl(WikidataUtil.getValue(text))
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subject, propertyStatement, objectValue, document.wikiPage.sourceIri, null)
                }
                else{
                  val objectValue = WikidataUtil.replaceSpace(WikidataUtil.getValue(text))
                  val datatype = if (WikidataUtil.getDatatype(v) != null) context.ontology.datatypes(WikidataUtil.getDatatype(v)) else null
                  quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, subject, propertyStatement, objectValue, document.wikiPage.sourceIri, datatype)
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
