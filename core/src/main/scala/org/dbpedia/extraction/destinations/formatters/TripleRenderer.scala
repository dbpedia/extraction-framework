package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad

class TripleRenderer(builder: => TripleBuilder)
extends (Quad => String) 
{
  def apply(quad: Quad): String = {
    
    val tb = builder
    
    tb.start(quad.context)
    
    tb.subjectUri(quad.subject)
    
    tb.predicateUri(quad.predicate)
    
    if (quad.datatype == null) tb.objectUri(quad.value)
    else if (quad.datatype.name == "xsd:string") tb.plainLiteral(quad.value, quad.language.isoCode)
    else tb.typedLiteral(quad.value, quad.datatype.uri)
    
    tb.end(quad.context)
    
    tb.toString()
  }
}