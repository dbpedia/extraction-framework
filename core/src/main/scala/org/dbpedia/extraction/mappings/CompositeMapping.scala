package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser.Node

/**
 * TODO: generic type may not be optimal.
 */
class CompositeMapping[N <: Node](mappings: Mapping[N] *) extends Mapping[N]
{
  override def extract(node : N, subjectUri : String, context : PageContext): Seq[Quad] =
  {
    mappings.flatMap(_.extract(node, subjectUri, context))
  }
}
