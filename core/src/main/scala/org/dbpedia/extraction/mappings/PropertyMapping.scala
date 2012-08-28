package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode

/**
 * Marker trait for mappings which map one or more properties of a specific class.
 * Necessary to make PropertyMappings distinguishable from other Mapping[TemplateNode] types.
 */
trait PropertyMapping extends Mapping[TemplateNode]
