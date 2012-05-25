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
    else if (quad.datatype.name == "xsd:string") builder.plainLiteral(quad.value, quad.language.isoCode)
    else builder.typedLiteral(quad.value, quad.datatype.uri)
    
    builder.end(quad.context)
    
    builder.result()
  }
}