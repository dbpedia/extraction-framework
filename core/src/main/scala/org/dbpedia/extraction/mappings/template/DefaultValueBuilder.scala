package org.dbpedia.extraction.mappings.template

import org.dbpedia.extraction.wikiparser.{TextNode, PropertyNode, TemplateNode}

/**
 *
 */
class DefaultValueBuilder(val property : String, val default : String)  extends PropertyValueBuilder {

  override def apply(node: TemplateNode): Option[PropertyNode] = {
    node.property(property) match {
      case None => Some(PropertyNode(property, List(TextNode(default, node.line)), node.line))
      case propertyNode => propertyNode
    }
  }
}
