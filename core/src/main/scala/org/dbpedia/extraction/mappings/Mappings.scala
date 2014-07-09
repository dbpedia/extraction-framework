package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{TemplateNode,TableNode}

/**
 */
class Mappings ( 
  val templateMappings : Map[String, Extractor[TemplateNode]],
  val tableMappings : List[Extractor[TableNode]]
)