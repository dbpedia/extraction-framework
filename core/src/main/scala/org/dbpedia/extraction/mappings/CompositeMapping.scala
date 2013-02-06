package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Dataset,Quad}

/**
 * TODO: generic type may not be optimal.
 */
class CompositeMapping[N](mappings: Mapping[N] *) extends Mapping[N]
{
  override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet

  override def extract(input: N, subjectUri: String, context: PageContext): Seq[Quad] = {
    mappings.flatMap(_.extract(input, subjectUri, context))
  }
}
