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
 * Created by ali on 2/28/15.
 * wikidata property page's aliases, descriptions, labels and statements are extracted.
 * wikidata nampespace used for property pages.
 *
 * aliases are extracted on the form of
 * wikidata:P102 dbo:alias "political party, party"@en .
 *
 * descriptions are extacted on the form of
 * wikidata:P102 dbo:description "the political party of which this politician is or has been a member"@en .
 *
 * labels are extracted on the form of
 * wikidata:P102 rdfs:label "member of political party"@en.
 *
 * statements are extracted on the form of
 * wikidata:P102 wikidata:P1646 wikidata:P580 .
 *
 *
 */
@SoftwareAgentAnnotation(classOf[WikidataPropertyExtractor], AnnotationType.Extractor)
class WikidataPropertyExtractor(
                                 context: {
                                   def ontology: Ontology
                                   def language: Language
                                 }
                                 )
  extends JsonNodeExtractor {

  private val aliasProperty = context.ontology.properties("alias")
  private val descriptionProperty = context.ontology.properties("description")
  private val labelProperty = context.ontology.properties("rdfs:label")

  override val datasets = Set(DBpediaDatasets.WikidataProperty)


  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val subject = WikidataUtil.getWikidataNamespace(subjectUri).replace("Property:", "")

    quads ++= getAliases(page, subject)
    quads ++= getDescriptions(page, subject)
    quads ++= getLabels(page, subject)
    quads ++= getStatements(page, subject)

    quads
  }


  private def getAliases(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace == Namespace.WikidataProperty) {
      for ((lang, value) <- page.wikiDataDocument.getAliases) {
        val alias = WikidataUtil.replacePunctuation(value.toString, lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataProperty, subjectUri, aliasProperty, alias,
              page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }

  private def getDescriptions(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace == Namespace.WikidataProperty) {
      for ((lang, value) <- page.wikiDataDocument.getDescriptions()) {
        val description = WikidataUtil.replacePunctuation(value.toString(), lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataProperty, subjectUri,
              descriptionProperty, description, page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }

  private def getLabels(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    if (page.wikiPage.title.namespace == Namespace.WikidataProperty) {
      for ((lang, value) <- page.wikiDataDocument.getLabels) {
        val literalWithoutLang = WikidataUtil.replacePunctuation(value.toString, lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataProperty,
              subjectUri, labelProperty, literalWithoutLang, page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }


  private def getStatements(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace == Namespace.WikidataProperty) {
      for (statementGroup <- page.wikiDataDocument.getStatementGroups) {
        statementGroup.foreach {
          statement => {
            val claim = statement.getClaim
            val property = WikidataUtil.getWikidataNamespace(claim.getMainSnak().getPropertyId().getIri)

            claim.getMainSnak() match {
              case mainSnak: ValueSnak => {
                val v = mainSnak.getValue
                val value = WikidataUtil.getValue(v)
                val datatype = if (WikidataUtil.getDatatype(v) != null) context.ontology.datatypes(WikidataUtil.getDatatype(v)) else null
                quads += new Quad(context.language, DBpediaDatasets.WikidataProperty, subjectUri, property, value, page.wikiPage.sourceIri, datatype)
              }
              case _ =>
            }
          }
        }
      }
    }

    quads
  }
}


