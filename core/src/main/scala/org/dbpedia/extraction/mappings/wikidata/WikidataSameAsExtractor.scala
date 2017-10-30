package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * it's an extractor to extract sameas data from DBpedia-WikiData on the form of
 * <http://wikidata.dbpedia.org/resource/Q18>  owl:sameAs <http://dbpedia.org/resource/London>
 * <http://wikidata.dbpedia.org/resource/Q18>  owl:sameAs <http://fr.dbpedia.org/resource/London>
 * <http://wikidata.dbpedia.org/resource/Q18>  owl:sameAs <http://co.dbpedia.org/resource/London>
 */
@SoftwareAgentAnnotation(classOf[WikidataSameAsExtractor], AnnotationType.Extractor)
class WikidataSameAsExtractor(
       context: {
         def ontology: Ontology
         def language: Language
       }
       )
  extends JsonNodeExtractor {
  // Here we define all the ontology predicates we will use
  private val sameAsProperty = context.ontology.properties("owl:sameAs")


  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataSameAs)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      val itemDocument: ItemDocument = page.wikiDataDocument.asInstanceOf[ItemDocument]

      for ((lang, siteLink) <- itemDocument.getSiteLinks) {
        val l = lang.toString().replace("wiki", "")
        try {
          Language.get(l) match {
            case Some(dbpedia_lang) => {
              val resourceIri = dbpedia_lang.resourceUri.append(siteLink.getPageTitle)
              quads += new Quad(context.language, DBpediaDatasets.WikidataSameAs,
                subjectUri, sameAsProperty, resourceIri, page.wikiPage.sourceIri, null)
            }
            case _ =>
          }
        }
        catch {
          case e: IllegalArgumentException => println(e)
        }

      }
    }

    quads
  }
}
