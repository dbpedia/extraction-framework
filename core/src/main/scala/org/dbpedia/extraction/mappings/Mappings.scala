package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.Node

/**
 */
class Mappings( 
  val templateMappings : Map[String, ClassMapping[Node]],
  val tableMappings : List[TableMapping] 
)