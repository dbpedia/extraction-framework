package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode}
import org.wikidata.wdtk.datamodel.interfaces._
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._


/**
 * Extracts Wikidata claims
 * on the form of
 * value triples:
 * <http://wikidata.dbpedia.org/resouce/Q64> <http://www.wikidata.org/entity/P625> "33.3333333 -123.433333333"
 * URI triples
 * <http://wikidata.dbpedia.org/resouce/Q64> <http://www.wikidata.org/entity/P625> <wikidata.dbpedia.org/resource/Q223>
 *
 */
class WikidataFactsExtractor(
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
  private val labelProperty = context.ontology.properties("rdfs:label")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataFacts)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()
    for ((statementGroup) <- page.wikiDataItem.getStatementGroups) {
      val claim = statementGroup.getStatements().get(0).getClaim()
      val propertyId = claim.getMainSnak().getPropertyId().toString().replace("(PropertyId)", "")

      if (claim.getMainSnak.isInstanceOf[ValueSnak]) {
        val mainSnak: ValueSnak = claim.getMainSnak().asInstanceOf[ValueSnak]
        if(mainSnak.getValue().isInstanceOf[ItemIdValue]) {
          val value:ItemIdValue = mainSnak.getValue().asInstanceOf[ItemIdValue]
          val s=value.toString().replace("(ItemId)","")
          quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, propertyId, s, page.wikiPage.sourceUri, null)
        } else if(mainSnak.getValue().isInstanceOf[StringValue]){
          val value:StringValue = mainSnak.getValue().asInstanceOf[StringValue]
          val s=value.toString().replace("(String)","")
          quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, propertyId, s, page.wikiPage.sourceUri, context.ontology.datatypes("xsd:string"))
        }
      }
    }
    quads
  }
}
