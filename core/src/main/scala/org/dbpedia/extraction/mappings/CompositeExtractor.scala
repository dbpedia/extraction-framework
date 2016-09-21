package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.extraction.destinations.Quad

/**
 * TODO: generic type may not be optimal.
 */
class CompositeExtractor[N](mappings: Extractor[N]*) extends Extractor[N]
{
  override val datasets: Set[Dataset] = mappings.flatMap(_.datasets).toSet

  override def extract(input: N, subjectUri: String, context: PageContext): Seq[Quad] = {
    mappings.flatMap(_.extract(input, subjectUri, context))
  }

  /**
    * Optional function called after all Wikipages were processed
    *
    * @param context The page context which holds the state of the extraction.
    * @return A graph holding extracted data added to the result graph of all Wikipages processed
    */
  override def postProcess(context: PageContext): Seq[Quad] = mappings.flatMap(_.postProcess(context))
}
