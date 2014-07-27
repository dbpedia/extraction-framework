package org.dbpedia.extraction.mappings


import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._
/**
 * Extracts labels triples from Wikidata sources
 * on the form of
 * http://data.dbpedia.org/Q64 rdfs:label "new York"@fr
 * http://data.dbpedia.org/Q64 rdfs:label "new York City"@en
 */
class WikidataLabelExtractor(
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
  private val labelProperty = context.ontology.properties("rdfs:label")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataLabels)

  override def extract(page: JsonNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    //for example :  skos:label  >> for labels extractor
    //               owl:sameas >> for  Language links

    for ((lang, value) <- page.wikiDataItem.getLabels) {
      Language.get(lang) match
      {
        case Some(l) => quads += new Quad(l, DBpediaDatasets.WikidataLabels, subjectUri, labelProperty, value.toString(), page.wikiPage.sourceUri, context.ontology.datatypes("rdf:langString"))
        case _=>
      }
    }
  quads
 }
}
