package org.dbpedia.extraction.mappings.template

import org.dbpedia.extraction.wikiparser.{TextNode, PropertyNode, TemplateNode}

/**
 *
 */
class ConstantValueBuilder(val property : String, val value : String) extends PropertyValueBuilder {

  override def apply(node: TemplateNode) : Option[PropertyNode] = {

    Some(PropertyNode(property, List(TextNode(value, node.line)), node.line))
  }
}
