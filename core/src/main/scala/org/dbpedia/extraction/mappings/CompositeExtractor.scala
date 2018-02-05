package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.transform.Quad

/**
 * TODO: generic type may not be optimal.
 */
@SoftwareAgentAnnotation(classOf[CompositeExtractor[_]], AnnotationType.Extractor)
class CompositeExtractor[N](mappings: Extractor[N]*) extends Extractor[N]
{
  override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet

  override def extract(input: N, subjectUri: String): Seq[Quad] = {
    mappings.flatMap(_.extract(input, subjectUri))
  }
}
