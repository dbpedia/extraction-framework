package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._

/**
 * Extractors are mappings that extract data from a PageNode.
 */
trait Extractor extends Mapping[PageNode]
