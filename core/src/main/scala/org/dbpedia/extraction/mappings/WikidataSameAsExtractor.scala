package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode, PageNode}
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._
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
  private val sameAsProperty = context.ontology.properties("owl:sameAs")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataSameAs)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()
    for ((lang,siteLink) <- page.wikiDataItem.getSiteLinks) {
      Language.get(lang.substring(0,2)) match{
        case Some(dbpedia_lang) => {
          val prefix = if (dbpedia_lang.wikiCode=="en") "" else dbpedia_lang.wikiCode+"."
          val suffix = siteLink.getPageTitle().toString().replace(" ","_")
          val objectUri = "http://" + prefix + "dbpedia.org/resource/" + suffix
          quads += new Quad(context.language, DBpediaDatasets.WikidataSameAs, subjectUri, sameAsProperty,objectUri, page.wikiPage.sourceUri,null)
        }
        case _=>
      }
   }
    quads
  }
}
