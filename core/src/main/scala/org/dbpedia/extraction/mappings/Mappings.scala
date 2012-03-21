package org.dbpedia.extraction.mappings

/**
 * TODO: store templateMappings and conditionalMappings in one map. MappingExtractor combines them anyway.
 */
class Mappings( val templateMappings : Map[String, TemplateMapping],
                val tableMappings : List[TableMapping],
                val conditionalMappings : Map[String, ConditionalMapping] )