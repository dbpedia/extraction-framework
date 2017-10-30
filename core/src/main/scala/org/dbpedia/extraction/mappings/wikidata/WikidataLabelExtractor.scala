package org.dbpedia.extraction.mappings


import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts labels triples from Wikidata sources
 * on the form of
 * http://data.dbpedia.org/Q64 rdfs:label "new York"@fr
 * http://data.dbpedia.org/Q64 rdfs:label "new York City"@en
 */
@SoftwareAgentAnnotation(classOf[WikidataLabelExtractor], AnnotationType.Extractor)
class WikidataLabelExtractor(
                              context: {
                                def ontology: Ontology
                                def language: Language
                              }
                              )
  extends JsonNodeExtractor {
  // Here we define all the ontology predicates we will use
  private val labelProperty = context.ontology.properties("rdfs:label")

  private val mappingLanguages = Namespace.mappingLanguages

  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.Labels,DBpediaDatasets.WikidataLabelsRest)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()
    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      for ((lang, value) <- page.wikiDataDocument.getLabels) {
        val literalWithoutLang = WikidataUtil.replacePunctuation(value.toString, lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            if (mappingLanguages.contains(dbpedia_lang))
              quads += new Quad(dbpedia_lang, DBpediaDatasets.Labels,
                subjectUri, labelProperty, literalWithoutLang, page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
            else
              quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataLabelsRest,
                subjectUri, labelProperty, literalWithoutLang, page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }
}
