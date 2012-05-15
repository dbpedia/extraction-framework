package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser.PageNode

class CompositeExtractor(extractors: Mapping[PageNode]*)
extends CompositeMapping[PageNode](extractors: _*)
with Extractor

/**
 * Creates new extractors.
 */
object CompositeExtractor
{
    /**
     * Creates a new extractor.
     * 
     * TODO: using reflection here loses compile-time type safety.
     *
     * @param extractors List of extractor classes to be instantiated
     * @param context Any type of object that implements the required parameter methods for the extractors
     */
    def load(classes: Seq[Class[_ <: Extractor]], context: AnyRef): Extractor =
    {
        val extractors = classes.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
        new CompositeExtractor(extractors: _*)
    }
}
