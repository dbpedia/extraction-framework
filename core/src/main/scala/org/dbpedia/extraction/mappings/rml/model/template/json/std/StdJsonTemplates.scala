package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.json.JsonTemplates

/**
  * Created by wmaroy on 12.08.17.
  */
class StdJsonTemplates(node : JsonNode) extends JsonTemplates {

  override def toString : String = {
    node.toString
  }

}
