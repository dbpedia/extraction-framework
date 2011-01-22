package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.destinations.Graph

/**
 * Base class of all mappings which map to a specific class
 */
trait ClassMapping
{
    def extract(node : Node, subjectUri : String, pageContext : PageContext) : Graph
}
