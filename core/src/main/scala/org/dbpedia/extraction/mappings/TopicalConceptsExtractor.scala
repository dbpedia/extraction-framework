package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.mappings.TopicalConceptsExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

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
@SoftwareAgentAnnotation(classOf[TopicalConceptsExtractor], AnnotationType.Extractor)
class TopicalConceptsExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
    private val skosSubjectProperty = context.ontology.properties("dct:subject")

    private val rdfTypeProperty = context.ontology.properties("rdf:type")

    private val skosSubjectClass = context.ontology.classes("skos:Concept")

    private val catMainTemplates = TopicalConceptsExtractorConfig.catMainTemplates;

    override val datasets = Set(DBpediaDatasets.TopicalConcepts)

    override def extract(page : PageNode, subjectUri : String): Seq[Quad] =
    {
        if (page.title.namespace == Namespace.Category)
        {
            val allTemplates = collectCatMains(page)

//            if (allTemplates.size>1)
//                println("Found more than one cat main. %s".format(page.title))

            try {
                val quads = allTemplates.flatMap{ template =>
                    val mainResource = context.language.resourceUri.append(template.property("1").get.retrieveText.get)
                    (new Quad(context.language,
                        DBpediaDatasets.TopicalConcepts,
                        subjectUri,
                        skosSubjectProperty,
                        mainResource,
                        template.sourceIri) ::
                    new Quad(context.language,
                        DBpediaDatasets.TopicalConcepts,
                        mainResource,
                        rdfTypeProperty,
                        skosSubjectClass.uri,
                        template.sourceIri)
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
