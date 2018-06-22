package org.dbpedia.extraction.mappings.rml.model.resource

/**
  * Represents a literal
  */
case class RMLLiteral(literal: String) {

  override def toString() = {
    literal
  }

}

