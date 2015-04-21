package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._

/**
 * Extractors are mappings that extract data from a JsonNode.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait JsonNodeExtractor extends Extractor[JsonNode]
