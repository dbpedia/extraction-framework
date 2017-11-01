package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.ExtractorUtils
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 *  Extracts structured data based on hand-generated mappings of Wikipedia infoboxes to the DBpedia ontology.
 */
@SoftwareAgentAnnotation(classOf[MappingExtractor], AnnotationType.Extractor)
class MappingExtractor(
  context : {
    def mappings : Mappings
    def redirects : Redirects
  }
)
extends PageNodeExtractor
{
  private val templateMappings = context.mappings.templateMappings
  private val tableMappings = context.mappings.tableMappings

  private val resolvedMappings = context.redirects.resolveMap(templateMappings)

  override val datasets: Set[Dataset] = templateMappings.values.flatMap(_.datasets).toSet ++ tableMappings.flatMap(_.datasets).toSet ++ Set(DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(page : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    val graph = extractNode(page, subjectUri)

    if (graph.isEmpty) Seq.empty
    else splitInferredFromDirectTypes(graph, page, subjectUri)
  }

  /**
   * Extracts a data from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def extractNode(node : Node, subjectUri : String) : Seq[Quad] =
  {
    //Try to extract data from the node itself
    val graph = node match
    {
      case templateNode : TemplateNode =>
      {
        resolvedMappings.get(templateNode.title.decoded) match
        {
          case Some(mapping) =>
            mapping.extract(templateNode, subjectUri)
          case None => Seq.empty
        }
      }
      case tableNode : TableNode =>
      {
        tableMappings.flatMap(_.extract(tableNode, subjectUri))
      }
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.
    if(graph.isEmpty)
    {
      node.children.flatMap(child => extractNode(child, subjectUri))
    }
    else
    {
      graph
    }
  }

  private def splitInferredFromDirectTypes(originalGraph: Seq[Quad], node : Node, subjectUri : String) : Seq[Quad] = {
    node.getAnnotation(TemplateMapping.CLASS_ANNOTATION) match {
      case None => {
        originalGraph
      }
      case Some(nodeClass) => {
        val adjustedGraph: Seq[Quad]=
          for (q <- originalGraph)
            yield
              // We split the types for the main resource only by checking the node annotations
              if (q.dataset.equals(DBpediaDatasets.OntologyTypes.encoded) &&
                  q.subject.equals(subjectUri) &&
                  !q.value.equals(nodeClass.toString) )
                q.copy(dataset = DBpediaDatasets.OntologyTypesTransitive.encoded)
              else q

        adjustedGraph
      }
    }
  }

}
