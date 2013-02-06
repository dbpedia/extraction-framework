package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import scala.collection.mutable.ArrayBuffer

/**
 * Extracts information about which concept is a category and how categories are related using the SKOS Vocabulary.
 */
class SkosCategoriesExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends Extractor
{
  private val rdfTypeProperty = context.ontology.properties("rdf:type")
  private val skosConceptClass = context.ontology.classes("skos:Concept")
  private val skosPrefLabelProperty = context.ontology.properties("skos:prefLabel")
  private val skosBroaderProperty = context.ontology.properties("skos:broader")
  private val skosRelatedProperty = context.ontology.properties("skos:related")
  private val language = context.language
  private val quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.SkosCategories, null) _
  
  override val datasets = Set(DBpediaDatasets.SkosCategories)

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Category) return Seq.empty

    var quads = new ArrayBuffer[Quad]()

    quads += new Quad(language, DBpediaDatasets.SkosCategories, subjectUri, rdfTypeProperty, skosConceptClass.uri, node.sourceUri)
    quads += new Quad(language, DBpediaDatasets.SkosCategories, subjectUri, skosPrefLabelProperty, node.title.decoded, node.sourceUri, new Datatype("xsd:string"))

    for(link <- collectCategoryLinks(node))
    {
      val property = link.destinationNodes match
      {
        // TODO: comment: What's going on here? What does it mean if text starts with ":"?
        case TextNode(text, _) :: Nil  if text.startsWith(":") => skosRelatedProperty
        case _ => skosBroaderProperty
      }
      
      val objectUri = language.resourceUri.append(link.destination.decodedWithNamespace)

      quads += quad(subjectUri, property, objectUri, link.sourceUri)
    }

    quads
  }

  private def collectCategoryLinks(node : Node) : List[InternalLinkNode] =
  {
    node match
    {
      case linkNode : InternalLinkNode if linkNode.destination.namespace == Namespace.Category => List(linkNode)
      case _ => node.children.flatMap(collectCategoryLinks)
    }
  }
}