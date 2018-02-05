package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.wikiparser.WikiPage

@SoftwareAgentAnnotation(classOf[CompositeWikiPageExtractor], AnnotationType.Extractor)
class CompositeWikiPageExtractor(extractors: Extractor[WikiPage]*)
extends CompositeExtractor[WikiPage](extractors: _*)
with WikiPageExtractor


