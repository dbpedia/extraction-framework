package org.dbpedia.extraction.mappings


import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import collection.mutable.ArrayBuffer
import  org.dbpedia.extraction.wikiparser.Namespace

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
  extends JsonNodeExtractor
{
  // Here we define all the ontology predicates we will use
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val labelProperty = context.ontology.properties("rdfs:label")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataLabels)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    //for each parser method exists a children node , you can differentiate between them through the Tiples property
    //for example :  skos:label  >> for labels extractor
    //               owl:sameas >> for  Language links
    for (n <- page.children) {

      n match {
        case node: JsonNode => {

          for (property <- node.getValueTriples.keys)
          {

            //check for triples that contains sameas properties only ie.(represents language links)
            node.NodeType match {
              case JsonNode.Labels => {

                //labels are in the form of valuesTriples so SimpleNode.getValueTriples method is used  which returns Map[String,String]
                val labelsMap = node.getValueTriples(property)
                for( lang <- labelsMap.keys)
                {
                  //write triples for languages that included only in the namespace
                  Language.get(lang) match
                  {
                    case Some(l) => quads += new Quad(l, DBpediaDatasets.WikidataLabels, subjectUri, labelProperty, labelsMap(lang), page.wikiPage.title.pageIri, context.ontology.datatypes("xsd:string"))
                    case _=>
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

//
//case node: WikidataInterWikiLinkNode => {
//val dst = node.destination
//if (dst.isInterLanguageLink) {
//val dstLang = dst.language
//val srcLang = node.source.language
//quads += quad(srcLang.resourceUri.append(node.source.decodedWithNamespace), dstLang.resourceUri.append(dst.decodedWithNamespace), node.sourceUri)
//}
//}