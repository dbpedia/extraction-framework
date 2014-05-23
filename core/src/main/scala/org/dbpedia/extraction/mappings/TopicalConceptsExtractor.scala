package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.config.mappings.TopicalConceptsExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, Quad}
import scala.language.reflectiveCalls

/**
 * Relies on Cat main templates. Goes over all categories and extract DBpedia Resources that are the main subject of that category.
 * We are using this to infer that a resource is a Topical Concept.
 *
 * TODO only do that for resources that have no other ontology type, in post-processing
 *
 * TODO check if templates Cat_exp, Cat_main_section, and Cat_more also apply
 *
 * @author pablomendes
 * @author maxjakob
 */
class TopicalConceptsExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
    /**
     * Don't access context directly in methods. Cache context.language for use inside methods so that
     * Spark (distributed-extraction-framework) does not have to serialize the whole context object
     */
    private val language = context.language

    private val skosSubjectProperty = context.ontology.properties("skos:subject")

    private val rdfTypeProperty = context.ontology.properties("rdf:type")

    private val skosSubjectClass = context.ontology.classes("skos:Concept")

    private val catMainTemplates = TopicalConceptsExtractorConfig.catMainTemplates;

    override val datasets = Set(DBpediaDatasets.TopicalConcepts)

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
    {
        if (page.title.namespace == Namespace.Category)
        {
            val allTemplates = collectCatMains(page)

//            if (allTemplates.size>1)
//                println("Found more than one cat main. %s".format(page.title))

            try {
                val quads = allTemplates.flatMap{ template =>
                    val mainResource = language.resourceUri.append(template.property("1").get.retrieveText.get)
                    (new Quad(language,
                        DBpediaDatasets.TopicalConcepts,
                        subjectUri,
                        skosSubjectProperty,
                        mainResource,
                        template.sourceUri) ::
                    new Quad(language,
                        DBpediaDatasets.TopicalConcepts,
                        mainResource,
                        rdfTypeProperty,
                        skosSubjectClass.uri,
                        template.sourceUri)
                    :: Nil)
                }

                return quads
            } catch {
                case e: Exception => println("TopicalConceptsExtractor failed for page %s.".format(page.title))
            }

        }

        Seq.empty
    }

    private def collectCatMains(node : Node) : List[TemplateNode] = node match
    {
        case catMainLinkNode : TemplateNode if catMainTemplates.contains(catMainLinkNode.title.decoded) && catMainLinkNode.property("1").nonEmpty => List(catMainLinkNode)
        case _ => node.children.flatMap(collectCatMains)
    }

}
