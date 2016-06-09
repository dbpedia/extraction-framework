package org.dbpedia.extraction.server.resources.rml

import org.dbpedia.extraction.mappings.Mappings
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.PageNode

/**
  * Factory that create RML Template Mappings converted from DBpedia mappings
  */
class RMLTemplateMappingFactory(node: PageNode, language: Language, mappings : Mappings) {

  def createMapping(): RMLTemplateMapping = {
    //TODO: implement
    null
  }

}
