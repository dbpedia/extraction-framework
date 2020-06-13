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

class WikidataLexemeExtractor(
                                 context: {
                                   def ontology: Ontology
                                   def language: Language
                                 }
                               )
  extends JsonNodeExtractor {

  private val lexicalCategoryProperty = "http://dbpedia.org/ontology/lexicalcategory"
  private val lemmaProperty = "http://dbpedia.org/ontology/lemma"
  private val language = "http://dbpedia.org/ontology/language"
  private val grammaticalFeatureProperty = "http://dbpedia.org/ontology/grammaticalfeature"
  private val labelProperty = context.ontology.properties("rdfs:label")
  // private val descriptionProperty = context.ontology.properties("description")
  //  private val languageProperty = context.ontology.properties("rdf:language")

  private val sameAsProperty = context.ontology.properties("owl:sameAs")
  override val datasets = Set(DBpediaDatasets.WikidataLexeme)


  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val subject = WikidataUtil.getWikidataNamespace(subjectUri).replace("Lexeme:", "")

    //checks if extractor is used for correct entity


    quads ++= getLemmas(page, subject)
    quads ++= getForms(page, subject)


    quads
  }



  private def getLemmas(document: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    val subjectString: String = "http://lex.dbpedia.org/resource/"
    if (document.wikiPage.title.namespace == Namespace.WikidataLexeme) {
      val page = document.wikiDataDocument.deserializeLexemeDocument(document.wikiPage.source)
      for ((lang, value) <- page.getLemmas) {
        val lemmas = subjectString+value.getText
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataLexeme, lemmas, sameAsProperty, subjectUri,
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
      for (form <- page.getForms){
        val formId = form.getEntityId.getId


        for (grammaticalFeature <- form.getGrammaticalFeatures){
          grammaticalFeature match{
            case value: Value =>{
              val objectValue = WikidataUtil.getValue(value)
              quads += new Quad(context.language, DBpediaDatasets.WikidataLexeme, formId, grammaticalFeatureProperty, objectValue,
                document.wikiPage.sourceIri, null)
            }
            case _ =>
          }
        }

      }

    }

    quads
  }

}