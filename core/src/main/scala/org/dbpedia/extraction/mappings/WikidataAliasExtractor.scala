package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.JsonNode
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * Created by ali on 7/29/14.
 */

class WikidataAliasExtractor (
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
  private val aliasProperty = context.ontology.properties("alias")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataAlias)

  override def extract(page: JsonNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    for ((lang, value) <- page.wikiDataItem.getAliases) {
      val alias = value.toString().replace("(" + lang + ")", "").replace("[","").replace("]","").trim()
      Language.get(lang) match
      {
        case Some(dbpedia_lang) => quads += new Quad(dbpedia_lang, DBpediaDatasets.WikidataAlias, subjectUri, aliasProperty,alias.toString(), page.wikiPage.sourceUri, context.ontology.datatypes("rdf:langString"))
        case _=>
      }
    }
    quads
  }
}

