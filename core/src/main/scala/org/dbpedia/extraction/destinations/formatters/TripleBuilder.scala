package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import java.net.{URI,URISyntaxException}

abstract class TripleBuilder {
  
  def render(quad: Quad): String = {
    
    start(quad.context)
    
    uri(quad.subject)
    
    uri(quad.predicate)
    
    if (quad.datatype == null) uri(quad.value)
    else if (quad.datatype.name == "xsd:string") string(quad.value, quad.language.isoCode)
    else value(quad.value, quad.datatype.uri)
    
    end(quad.context)
    
    toString()
  }
    
  def start(context: String): Unit
  
  def uri(uri: String): Unit
  
  def string(value: String, isoLang: String): Unit
  
  def value(value: String, datatype: String): Unit
  
  def end(context: String): Unit
}