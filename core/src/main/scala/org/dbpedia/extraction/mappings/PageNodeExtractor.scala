package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._

/**
 * Extractors are mappings that extract data from a PageNode.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait PageNodeExtractor extends Extractor[PageNode] {

  def extract(page: PageNode): Seq[Quad] = {
    if (this.state != ExtractorState.Finalized)
      this.extract(page, page.uri, new PageContext())
    else
      throw new IllegalStateException("Attempted extraction with finalized extractor.")
  }
}