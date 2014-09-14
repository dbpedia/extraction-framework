package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Dataset, Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{WikiTitle, JsonNode, Namespace, PageNode}
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._
import org.dbpedia.extraction.wikiparser.Namespace

/*
* Extract Wikidata sitelinks on the form of
*   <http://L1.dbpedia.org/resource/xxx> owl:sameAs <http://L2.dbpedia.org/resource/xxx> .
*Sample:
*   <http://dbpedia.org/resource/Lithuania> owl:sameAs  <http://mt.dbpedia.org/resource/Litwanja>  .
*   <http://dbpedia.org/resource/Lithuania> ow:sameAs <http://yi.dbpedia.org/resource/ליטע>  .
*   <http://dbpedia.org/resource/Lithuania> owl:sameAs <http://sk.dbpedia.org/resource/Južná_Amerika> .
**/

class WikidataLLExtractor(
                           context : {
                             def ontology : Ontology
                             def language : Language
                           }
                           )
  extends JsonNodeExtractor
{
  // Here we define all the ontology predicates we will use
  private val sameAsProperty = context.ontology.properties("owl:sameAs")


  // this is where we will store the output
  override val datasets = for (lang <-Namespace.mappings.keySet) yield new Dataset("wikidata-ll-"+lang.wikiCode)

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    for (lang<-Namespace.mappings.keySet) {
      for ((wikidataLang, siteLink) <- page.wikiDataItem.getSiteLinks) {
        for ((wikidataLang2, siteLink2) <- page.wikiDataItem.getSiteLinks) {
          if (wikidataLang != wikidataLang2) {
            val l1 = wikidataLang.toString().replace("wiki", "")
            val l2 = wikidataLang2.toString().replace("wiki", "")
            if (lang.wikiCode == l1) {
              val sitelink1 = WikiTitle.parse(siteLink.getPageTitle().toString(), lang)
              Language.get(l2) match {
                case Some(dbpedia_lang) => {
                  val sitelink2 = WikiTitle.parse(siteLink2.getPageTitle().toString(), dbpedia_lang)
                  val dataset = Map(lang.wikiCode -> new Dataset("wikidata-ll-"+lang.wikiCode))
                  quads += new Quad(context.language, dataset(lang.wikiCode), sitelink1.resourceIri,
                    sameAsProperty, sitelink2.resourceIri, page.wikiPage.sourceUri,null)
                }
                case _ =>
              }
            }
          }
        }
      }
    }
    quads
  }
}
