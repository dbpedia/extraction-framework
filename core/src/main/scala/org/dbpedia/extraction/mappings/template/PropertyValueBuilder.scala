package org.dbpedia.extraction.mappings.template

import org.dbpedia.extraction.wikiparser.{PropertyNode, TemplateNode}

/**
 *
 */
trait PropertyValueBuilder {

  val property : String

  def apply(node: TemplateNode) : Option[PropertyNode]
}

object PropertyValueBuilder {

  implicit def propertyValueBuilder2String(p: PropertyValueBuilder) = p.property
}

