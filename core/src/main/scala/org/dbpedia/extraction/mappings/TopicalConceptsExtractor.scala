package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.destinations.{Dataset, Graph, Quad}
import org.dbpedia.extraction.util.{WikiUtil, Language}

/**
 * Relies on Cat main templates. Goes over all categories and extract DBpedia Resources that are the main subject of that category.
 * We are using this to infer that a resource is a Topical Concept.
 *
 * TODO we currently just assert that "?category skos:subject ?resource". Perhaps also add "?resource rdf:type dbpedia-owl:TopicalConcept". Perhaps only do that for resources that have no other ontology type.
 *
 * @author pablomendes
 * @blessing maxjakob
 */
class TopicalConceptsExtractor( context : {
                                   def ontology : Ontology
                                   def language : Language } ) extends Extractor
{
    val skosSubjectProperty = context.ontology.getProperty("skos:subject")
                              .getOrElse(throw new NoSuchElementException("Ontology property 'skos:subject' does not exist in DBpedia Ontology."))

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if (page.title.namespace == WikiTitle.Namespace.Category)
        {
            val allTemplates = collectCatMains(page)

//            if (allTemplates.size>1)
//                println("Found more than one cat main. %s".format(page.title))

            try {

                //TODO perhaps more useful if we constrain to only DBpedia resources that are not in any other class.
                val quads = allTemplates.map{template =>
                    new Quad(context.language,
                        new Dataset("cat_mains"),
                        subjectUri,
                        skosSubjectProperty,
                        OntologyNamespaces.getResource(WikiUtil.wikiEncode(template.property("1").get.retrieveText.get), context.language),
                        template.sourceUri,
                        null)
                }
                return new Graph(quads)
            } catch {
                case e: Exception => println("Cat main extraction failed for page %s.".format(page.title))
            }

        }

        new Graph()
    }

    private def collectCatMains(node : Node) : List[TemplateNode] = node match
    {
        case catMainLinkNode : TemplateNode if catMainLinkNode.title.decoded == "Cat main" && catMainLinkNode.property("1").nonEmpty => List(catMainLinkNode)
        case commonsCatLinkNode : TemplateNode if commonsCatLinkNode.title.decoded == "Commons cat" && commonsCatLinkNode.property("1").nonEmpty => List(commonsCatLinkNode)
        case _ => node.children.flatMap(collectCatMains)
    }

}

