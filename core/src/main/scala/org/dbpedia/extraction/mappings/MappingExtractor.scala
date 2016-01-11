package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import scala.language.reflectiveCalls

/**
 *  Extracts structured data based on hand-generated mappings of Wikipedia infoboxes to the DBpedia ontology.
 */
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

  override val datasets = templateMappings.values.flatMap(_.datasets).toSet ++ tableMappings.flatMap(_.datasets).toSet ++ Set(DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    val graph = extractNode(page, subjectUri, pageContext)

    if (graph.isEmpty) Seq.empty
    else splitInferredFromDirectTypes(graph, page, subjectUri)
  }

  /**
   * Extracts a data from a node.
   * Recursively traverses it children if the node itself does not contain any useful data.
   */
  private def extractNode(node : Node, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    //Try to extract data from the node itself
    val graph = node match
    {
      case templateNode : TemplateNode =>
      {
        resolvedMappings.get(templateNode.title.decoded) match
        {
          case Some(mapping) => mapping.extract(templateNode, subjectUri, pageContext)
          case None => Seq.empty
        }
      }
      case tableNode : TableNode =>
      {
        tableMappings.flatMap(_.extract(tableNode, subjectUri, pageContext))
      }
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.
    if(graph.isEmpty)
    {
      node.children.flatMap(child => extractNode(child, subjectUri, pageContext))
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
              if (q.dataset.equals(DBpediaDatasets.OntologyTypes.name) &&
                  q.subject.equals(subjectUri) &&
                  !q.value.equals(nodeClass.toString) )
                q.copy(dataset = DBpediaDatasets.OntologyTypesTransitive.name)
              else q

        adjustedGraph
      }
    }
  }

}
