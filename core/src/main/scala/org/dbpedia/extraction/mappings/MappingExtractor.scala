package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 *  Extracts structured data based on hand-generated mappings of Wikipedia infoboxes to the DBpedia ontology.
 */
class MappingExtractor( context : {
                            def mappings : Mappings
                            def redirects : Redirects } ) extends Extractor
{
    private val templateMappings = context.mappings.templateMappings
    private val tableMappings = context.mappings.tableMappings

    private val resolvedMappings = context.redirects.resolveMap(templateMappings)

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(page.title.namespace != Namespace.Main) return Seq.empty

        extractNode(page, subjectUri, pageContext)
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
}