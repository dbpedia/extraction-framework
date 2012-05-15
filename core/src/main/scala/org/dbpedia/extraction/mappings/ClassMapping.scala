package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.Node

/**
 * Marker trait for mappings which map to a specific class
 * TODO: remove?
 */
trait ClassMapping[N <: Node] extends Mapping[N]
