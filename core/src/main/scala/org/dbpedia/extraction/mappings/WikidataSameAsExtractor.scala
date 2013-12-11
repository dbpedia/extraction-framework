package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import collection.mutable.ArrayBuffer

/**
 * it's an extractor to extract sameas data from DBpedia-WikiData on the form of
 * <http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://dbpedia.org/resource/London>
 * <http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://fr.dbpedia.org/resource/London>
 * <http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://co.dbpedia.org/resource/London>
 */
class WikidataSameAsExtractor(
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
  override val datasets = Set(DBpediaDatasets.WikidataSameAs)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    //in the languagelinks case simplenode contains 1 output
    for (n <- page.children) {
      n match {
        case node: JsonNode => {
          for (property <- node.getUriTriples.keys)
          {
            //check for triples that contains sameas properties only ie.(represents language links)
            property match {
              case "http://www.w3.org/2002/07/owl#sameAs" => {

                //make combinations for each language and write Quads in the form :
                //<http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://dbpedia.org/resource/London>
                //<http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://fr.dbpedia.org/resource/London>
                //<http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://co.dbpedia.org/resource/London>
                // ..etc
                //links returned from the wikiparser are in the form of URIs so SimpleNode.getUriTriples method is used
                for( llink <- node.getUriTriples(property))
                {
                    quads += new Quad(context.language, DBpediaDatasets.WikidataSameAs, subjectUri, property,llink, page.wikiPage.title.pageIri,null)
                }
              }
              case _=> //ignore others
            }
          }
        }

        case _=>

    }
    }

    quads
  }
}
