package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode

/**
 * Marker trait for mappings which map one or more properties of a specific class
 */
trait PropertyMapping extends Mapping[TemplateNode]
