package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.JsonNode
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls


/**
 * Created by ali on 7/29/14.
 * Extracts descriptions triples from Wikidata sources
 * on the form of <http://wikidata.dbpedia.org/resource/Q139> <http://dbpedia.org/ontology/description> "description"@lang.
 */
class WikidataDescriptionExtractor (
                                     context : {
                                       def ontology : Ontology
                                       def language : Language
                                     }
                                     )
  extends JsonNodeExtractor {
  // Here we define all the ontology predicates we will use
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val aliasProperty = context.ontology.properties("description")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataDescription)

  override def extract(page: JsonNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    for ((lang, value) <- page.wikiDataItem.getDescriptions()) {
      val description = value.toString().replace("(" + lang + ")", "").trim()
      Language.get(lang) match
      {
        case Some(dbpedia_lang) => quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataDescription, subjectUri, aliasProperty,description, page.wikiPage.sourceUri, context.ontology.datatypes("rdf:langString"))
        case _=>
      }
    }
    quads
  }
}

