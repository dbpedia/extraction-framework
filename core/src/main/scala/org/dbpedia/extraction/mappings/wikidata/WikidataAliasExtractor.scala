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
 * Created by ali on 7/29/14.
 * Extracts aliases triples from Wikidata sources
 * on the form of
 * <http://wikidata.dbpedia.org/resource/Q446> <http://dbpedia.org/ontology/alias> "alias"@lang .
 */

@SoftwareAgentAnnotation(classOf[WikidataAliasExtractor], AnnotationType.Extractor)
class WikidataAliasExtractor(
                              context: {
                                def ontology: Ontology
                                def language: Language
                              }
                              )
  extends JsonNodeExtractor {
  // Here we define all the ontology predicates we will use
  private val aliasProperty = context.ontology.properties("alias")

  private val mappingLanguages = Namespace.mappingLanguages

  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataAliasMappingsWiki, DBpediaDatasets.WikidataAliasRest)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      for ((lang, value) <- page.wikiDataDocument.getAliases) {
        val alias = WikidataUtil.replacePunctuation(value.toString,lang)
        Language.get(lang) match {
          case Some(dbpedia_lang) => {
            if (mappingLanguages.contains(dbpedia_lang))
              quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataAliasMappingsWiki, subjectUri, aliasProperty, alias,
                page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
            else
              quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataAliasRest, subjectUri, aliasProperty, alias,
                page.wikiPage.sourceIri, context.ontology.datatypes("rdf:langString"))
          }
          case _ =>
        }
      }
    }
    quads
  }
}

