package org.dbpedia.extraction.mappings.rml.model.template.json.std

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.json.JsonTemplate

/**
  * Created by wmaroy on 12.08.17.
  */
abstract class AbstractStdJsonTemplate(_node: JsonNode) extends JsonTemplate {

  def node : JsonNode = _node

  override def toString : String = {
    _node.toString
  }

}
