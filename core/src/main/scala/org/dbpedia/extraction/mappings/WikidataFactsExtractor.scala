package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

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
  /**
   * Don't access context directly in methods. Cache context.language and context.ontology for use inside
   * methods so that Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language
  private val ontology = context.ontology

  // Here we define all the ontology predicates we will use
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val labelProperty = context.ontology.properties("rdfs:label")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataFacts)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    //for each parser method exists a children node, you can differentiate between them through the Tiples property
    //for example :  skos:label  >> for labels extractor
    //               owl:sameas >> for  Language links
    for (n <- page.children) {

      n match {
        case node: JsonNode => {

          //Generating Quads for ValueTriples
          for (property <- node.getValueTriples.keys)
          {

            //check for triples that doesn't contain Label or sameas properties only
              if(node.NodeType == JsonNode.Facts){

                val valueFacts = node.getValueTriples(property)
                for( fact <- valueFacts.keys)
                {
                  if(valueFacts(fact)=="")
                    quads += new Quad(null, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri, ontology.datatypes("xsd:string"))
                  else if (valueFacts(fact) == "CommonMediaFile")
                    quads += new Quad(language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri, null)
                  else
                    quads += new Quad(language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri, ontology.datatypes(valueFacts(fact)))
                }
              }
          }

          //Generating Quads for Uri
          for (property <- node.getUriTriples.keys)
          {
            //check for triples that doesn't contain Label or sameas properties only
            if(node.NodeType == JsonNode.Facts){

              //labels are in the form of valuesTriples so SimpleNode.getValueTriples method is used  which returns Map[String,String]
              val UriFacts = node.getUriTriples(property)
              for( fact <- UriFacts)
              {
                quads += new Quad(language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri, null)
              }
            }
          }



        }

        case _=>

    }
    }

    quads
  }
}
