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
  private val quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.SkosCategories, null) _
  
  override val datasets = Set(DBpediaDatasets.SkosCategories)

  override def extract(node : PageNode, subjectUri : String): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Category) return Seq.empty

    val softwareAgentUri = SoftwareAgentAnnotation.getAnnotationIri(this.getClass)

    var quads = new ArrayBuffer[Quad]()

    quads += new Quad(language, DBpediaDatasets.SkosCategories, subjectUri, rdfTypeProperty, skosConceptClass.uri, node.sourceIri)
    quads += new Quad(language, DBpediaDatasets.SkosCategories, subjectUri, skosPrefLabelProperty, node.title.decoded, node.sourceIri, new Datatype("rdf:langString"))

    for(link <- collectCategoryLinks(node))
    {
      val property = link.destinationNodes match
      {
        // TODO: comment: What's going on here? What does it mean if text starts with ":"?
        case TextNode(text, _, _) :: Nil  if text.startsWith(":") => skosRelatedProperty
        case _ => skosBroaderProperty
      }
      
      val objectUri = language.resourceUri.append(link.destination.decodedWithNamespace)

      val q = quad(subjectUri, property, objectUri, link.sourceIri)
      q.setProvenanceRecord(new DBpediaMetadata(
        node.getNodeRecord,
        if(q.dataset != null) Seq(RdfNamespace.fullUri(DBpediaNamespace.DATASET, q.dataset)) else Seq(),
        Some(ExtractorRecord(
          softwareAgentUri.toString,
          ParserRecord(
            softwareAgentUri.toString, //the software agent creating the object value is the same as the extractor
            link.root.getOriginWikiText(link.line),
            link.toWikiText,
            objectUri
          ),
          None, None, None, None
        )),
        None,
        Seq.empty
      ))
      quads += q
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