package org.dbpedia.extraction.mappings.template

import org.dbpedia.extraction.wikiparser.{PropertyNode, TemplateNode}

/**
 *
 */
class IdentityValueBuilder(val property: String) extends PropertyValueBuilder {

  override def apply(node: TemplateNode): Option[PropertyNode] = { node.property(property) }
}
