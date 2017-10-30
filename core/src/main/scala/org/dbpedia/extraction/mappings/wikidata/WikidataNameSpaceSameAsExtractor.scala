package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
* it's an extractor to extract Mappings between Wikidata URIs to WikiData URIs inside DBpedia, in the form of :
* <http://wikidata.dbpedia.org/resource/Q18>  <owl:sameas> <http://www.wikidata.org/entity/Q18>
*/
@SoftwareAgentAnnotation(classOf[WikidataNameSpaceSameAsExtractor], AnnotationType.Extractor)
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

  override def extract(page : JsonNode, subjectUri : String): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      val objectUri = subjectUri.replace(WikidataUtil.wikidataDBpNamespace,"http://www.wikidata.org/entity/")

      quads += new Quad(context.language, DBpediaDatasets.WikidataNameSpaceSameAs , subjectUri, sameAsProperty , objectUri, page.wikiPage.sourceIri,null)
    }

    quads
  }
}
