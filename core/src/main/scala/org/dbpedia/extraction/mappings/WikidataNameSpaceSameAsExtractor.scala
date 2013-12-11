package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import collection.mutable.ArrayBuffer

/**
 * it's an extractor to extract Mappings between Wikidata URIs to WikiData URIs inside DBpedia, in the form of :
 * <http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://wikidata.org/entity/Q18>
 */
class WikidataNameSpaceSameAsExtractor(
                         context : {
                           def ontology : Ontology
                           def language : Language
                         }
                         )
  extends JsonNodeExtractor
{
  // Here we define all the ontology predicates we will use
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  //private val sameasProperty = context.ontology.properties("")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataNameSpaceSameAs )

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()
    val property = "http://www.w3.org/2002/07/owl#sameAs"
    val objectUri = subjectUri.replace("wikidata.dbpedia.org/resource","wikidata.org/entity")
    quads += new Quad(context.language, DBpediaDatasets.WikidataNameSpaceSameAs , subjectUri, property , objectUri, page.wikiPage.title.pageIri,null)

    quads
  }
}
