package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.TemplateNode

@SoftwareAgentAnnotation(classOf[ConditionalMapping], AnnotationType.Extractor)
class ConditionalMapping(
  val cases : List[ConditionMapping], // must be public val for statistics
  val defaultMappings : List[PropertyMapping] // must be public val for statistics
)
extends Extractor[TemplateNode]
{
  override val datasets = cases.flatMap(_.datasets).toSet ++ defaultMappings.flatMap(_.datasets).toSet

  override def extract(node: TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    for(condition <- cases)
    {
      if (condition.matches(node)) {
        val graph = condition.extract(node, subjectUri)
        // template mapping sets instance URI
        val instanceURI = node.getAnnotation(TemplateMapping.INSTANCE_URI_ANNOTATION).getOrElse(throw new IllegalArgumentException("missing instance URI"))
        return graph ++ defaultMappings.flatMap(_.extract(node, instanceURI))
      }
    }
    Seq.empty
  }
}
