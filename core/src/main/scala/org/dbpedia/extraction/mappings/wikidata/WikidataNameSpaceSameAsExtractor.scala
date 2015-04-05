package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{WikidataUtil, Language}
import org.dbpedia.extraction.wikiparser.{Namespace, JsonNode}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

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
  private val sameAsProperty = context.ontology.properties("owl:sameAs")

  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataNameSpaceSameAs )

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      val objectUri = subjectUri.replace(WikidataUtil.wikidataDBpNamespace,"http://wikidata.org/entity/")

      quads += new Quad(context.language, DBpediaDatasets.WikidataNameSpaceSameAs , subjectUri, sameAsProperty , objectUri, page.wikiPage.sourceUri,null)
    }

    quads
  }
}
