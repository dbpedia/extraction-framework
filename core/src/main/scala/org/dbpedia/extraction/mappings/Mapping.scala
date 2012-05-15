package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.destinations.Quad

/**
 * TODO: generic type may not be optimal.
 */
trait Mapping[-N <: Node] {
  
    /**
     * @param page The source node
     * @param subjectUri The subject URI of the generated triples
     * @param context The page context which holds the state of the extraction.
     * @return A graph holding the extracted data
     */
    def extract(node: N, subjectUri: String, context: PageContext): Seq[Quad]

}