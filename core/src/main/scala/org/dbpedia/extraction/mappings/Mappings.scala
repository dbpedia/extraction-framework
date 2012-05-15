package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{TemplateNode,TableNode}

/**
 */
class Mappings ( 
  val templateMappings : Map[String, Mapping[TemplateNode]],
  val tableMappings : List[Mapping[TableNode]] 
)