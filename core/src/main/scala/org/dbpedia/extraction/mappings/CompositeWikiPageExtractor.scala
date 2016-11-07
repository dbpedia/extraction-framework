package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.sources.WikiPage

class CompositeWikiPageExtractor(extractors: Extractor[WikiPage]*)
extends CompositeExtractor[WikiPage](extractors: _*)
with WikiPageExtractor


