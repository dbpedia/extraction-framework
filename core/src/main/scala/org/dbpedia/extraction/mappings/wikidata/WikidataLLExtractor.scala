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

  private val mappingLanguages = Namespace.mappings.keySet
  private val datasetMap:Map[String,Dataset]= (for (lang <-mappingLanguages) yield (lang.wikiCode -> new Dataset("wikidata-ll-"+lang.wikiCode)))(collection.breakOut)
  override val datasets = datasetMap.values.toSet

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    for ((wikidataLang, siteLink) <- page.wikiDataItem.getSiteLinks) {
      val lang1 = wikidataLang.toString().replace("wiki", "")
      if (datasetMap.keys.contains(lang1)) {
        for ((wikidataLang2, siteLink2) <- page.wikiDataItem.getSiteLinks) {
          val lang2 = wikidataLang2.toString().replace("wiki", "")
          if (datasetMap.keys.contains(lang2) & lang1 != lang2) {
            Language.get(lang1) match {
              case Some(dbpedia_lang1) => {
                Language.get(lang2) match {
                  case Some(dbpedia_lang2) => {
                    val sitelink1 = WikiTitle.parse(siteLink.getPageTitle().toString(), dbpedia_lang1)
                    val sitelink2 = WikiTitle.parse(siteLink2.getPageTitle().toString(), dbpedia_lang2)
                    quads += new Quad(context.language, datasetMap(lang1), sitelink1.resourceIri,
                      sameAsProperty, sitelink2.resourceIri, page.wikiPage.sourceUri, null)
                  }
                  case _ =>
                }
              }
              case _ =>
            }
          }
        }
      }
    }

    quads
  }
}
