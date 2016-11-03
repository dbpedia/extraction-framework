package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.sources.WikiPage

/**
 * Extractors are mappings that extract data from a WikiPage.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait WikiPageExtractor extends Extractor[WikiPage] with ExtractionRecorder{
  def extract(page:WikiPage):Seq[Quad] = {
    if(this.state != ExtractorState.Finalized)
      this.extract(page, page.uri, new PageContext())
    else
      throw new IllegalStateException("Attempted extraction with finalized extractor.")
  }

  override def finalizeExtractor(): Unit = {
    this.finalizeRecorder()
    super.finalizeExtractor()
  }
}
