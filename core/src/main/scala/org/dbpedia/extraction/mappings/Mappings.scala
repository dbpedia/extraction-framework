package org.dbpedia.extraction.mappings


class Mappings( val templateMappings : Map[String, TemplateMapping],
                val tableMappings : List[TableMapping],
                val conditionalMappings : Map[String, ConditionalMapping] )