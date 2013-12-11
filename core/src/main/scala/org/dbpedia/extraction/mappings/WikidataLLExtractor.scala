package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace, PageNode}
import collection.mutable.ArrayBuffer

/**
 * Extracts data from Wikidata sources.
 * This is a copy of WikiPageExtractor for now with comments
 */
class WikidataLLExtractor(
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
  override val datasets = Set(DBpediaDatasets.WikidataLL)

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
            node.NodeType match {
              case  JsonNode.LanguageLinks => {

                //make combinations for each language and write Quads in the form :
                // fr.dbpedia:New_york  owl:sameas   en.dbpedia:New_york_City
                // fr.dbpedia:New_york  owl:sameas   ar.dbpedia:نيويورك
                // en.dbpedia:New_york_City owl:sameas   ar.dbpedia:نيويورك
                // en.dbpedia:New_york_City owl:sameas   fr.dbpedia:New_york
                // ..etc
                //Language links are in the form of URIs so SimpleNode.getUriTriples method is used
                for( llink <- node.getUriTriples(property))
                {
                  for (llink2 <- node.getUriTriples(property) diff List(llink))
                  {
                    quads += new Quad(context.language, DBpediaDatasets.WikidataLL, llink, property,llink2, page.wikiPage.title.pageIri,null)
                  }
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
