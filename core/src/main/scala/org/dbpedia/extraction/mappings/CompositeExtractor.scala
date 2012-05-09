package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.wikiparser.{PageNode}

class CompositeExtractor(extractors : Traversable[Extractor]) extends Extractor
{
    def this(extractors : Extractor*) = this(extractors)
    
    require(extractors.nonEmpty, "no extractors")
    
    override def extract(node : PageNode, subjectUri : String, context : PageContext) : Graph =
    {
        extractors.map(extractor => extractor.extract(node, subjectUri, context))
                  .reduceLeft(_ merge _)
    }
}
