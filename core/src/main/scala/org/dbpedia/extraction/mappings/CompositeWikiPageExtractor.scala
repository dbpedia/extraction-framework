package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.PageNode

class CompositeWikiPageExtractor(extractors: Extractor[WikiPage]*)
extends CompositeExtractor[WikiPage](extractors: _*)
with WikiPageExtractor {
  /**
    * @param page       The source node
    * @param subjectUri The subject URI of the generated triples
    * @return A graph holding the extracted data
    */
  override def extract(input: PageNode, subjectUri: String): Seq[Quad] = {
    input match{
      case wiki: WikiPage => extractors.flatMap(_.extract(input.asInstanceOf[WikiPage], subjectUri))
      case mode: PageNode => ???
    }
  }
}


