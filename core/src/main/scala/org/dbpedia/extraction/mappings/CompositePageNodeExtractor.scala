package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser.PageNode

class CompositePageNodeExtractor(extractors: Extractor[PageNode]*)
extends CompositeExtractor[PageNode](extractors: _*)
with PageNodeExtractor

/**
 * Creates new extractors.
 */
object CompositePageNodeExtractor
{
    /**
     * Creates a new extractor.
     * 
     * TODO: using reflection here loses compile-time type safety.
     *
     * @param extractors List of extractor classes to be instantiated
     * @param context Any type of object that implements the required parameter methods for the extractors
     */
    def load(classes: Seq[Class[_ <: PageNodeExtractor]], context: AnyRef): PageNodeExtractor =
    {
        val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
        new CompositePageNodeExtractor(extractors: _*)
    }
}
