package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance._
import org.dbpedia.extraction.ontology.{DBpediaNamespace, Ontology, RdfNamespace}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts information about which concept is a category and how categories are related using the SKOS Vocabulary.
 */
@SoftwareAgentAnnotation(classOf[SkosCategoriesExtractor], AnnotationType.Extractor)
class SkosCategoriesExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val rdfTypeProperty = context.ontology.properties("rdf:type")
  private val skosConceptClass = context.ontology.classes("skos:Concept")
  private val skosPrefLabelProperty = context.ontology.properties("skos:prefLabel")
  private val skosBroaderProperty = context.ontology.properties("skos:broader")
  private val skosRelatedProperty = context.ontology.properties("skos:related")
  private val language = context.language
  private val qb = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.SkosCategories, null)
  
  override val datasets = Set(DBpediaDatasets.SkosCategories)

  override def extract(node : PageNode, subjectUri : String): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Category) return Seq.empty

    var quads = new ArrayBuffer[Quad]()

    //generic values
    qb.setNodeRecord(node.getNodeRecord)
    qb.setSubject(subjectUri)
    qb.setSourceUri(node.sourceIri)

    //we need separate QB to ensure correctness
    val initQb = qb.clone
    initQb.setExtractor(this.softwareAgentAnnotation)

    initQb.setTriple(subjectUri, rdfTypeProperty, skosConceptClass.uri)
    quads += initQb.getQuad

    initQb.setTriple(subjectUri, skosPrefLabelProperty, node.title.decoded, new Datatype(Quad.langString))
    quads += initQb.getQuad

    for(link <- collectCategoryLinks(node))
    {
      val property = link.destinationNodes match
      {
        // TODO: comment: What's going on here? What does it mean if text starts with ":"?
        case TextNode(text, _, _) :: Nil  if text.startsWith(":") => skosRelatedProperty
        case _ => skosBroaderProperty
      }
      
      val objectUri = language.resourceUri.append(link.destination.decodedWithNamespace)

      qb.setExtractor(ExtractorRecord(
        this.softwareAgentAnnotation,
        Seq(ParserRecord(
          this.softwareAgentAnnotation,     //the software agent creating the object value is the same as the extractor
          link.root.getOriginWikiText(link.line),
          link.toWikiText,
          objectUri
        ))
      ))
      qb.setTriple(subjectUri, property, objectUri)
      qb.setSourceUri(link.sourceIri)

      quads += qb.getQuad
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