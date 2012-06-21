package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad

abstract class TripleFormatter(factory: () => TripleBuilder)
extends Formatter
{
  def render(quad: Quad): String = {
    
    val builder = factory()
    
    builder.start(quad.context)
    
    builder.subjectUri(quad.subject)
    
    builder.predicateUri(quad.predicate)
    
    if (quad.datatype == null) builder.objectUri(quad.value)
    else if (quad.datatype == "http://www.w3.org/2001/XMLSchema#string") builder.plainLiteral(quad.value, quad.language)
    else builder.typedLiteral(quad.value, quad.datatype)
    
    builder.end(quad.context)
    
    builder.result()
  }
}