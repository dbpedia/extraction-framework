package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.dataparser.StringParser

class ConditionalMapping( 
  val cases : List[ConditionMapping], // must be public val for statistics
  val defaultMappings : List[PropertyMapping] // must be public val for statistics
)
extends Mapping[TemplateNode]
{
  override val datasets = cases.flatMap(_.datasets).toSet ++ defaultMappings.flatMap(_.datasets).toSet

  override def extract(node: TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    for(condition <- cases)
    {
      if (condition.matches(node)) {
        val graph = condition.extract(node, subjectUri, pageContext)
        // template mapping sets instance URI
        val instanceURI = node.getAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing instance URI"))
        return graph ++ defaultMappings.flatMap(_.extract(node, instanceURI, pageContext))
      }
    }
    Seq.empty
  }
}
