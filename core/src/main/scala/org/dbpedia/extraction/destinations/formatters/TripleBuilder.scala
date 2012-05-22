package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad

trait TripleBuilder {
  
  def start(context: String): Unit
  
  def subjectUri(context: String): Unit
  
  def predicateUri(context: String): Unit
  
  def objectUri(context: String): Unit
  
  def plainLiteral(value: String, isoLang: String): Unit
  
  def typedLiteral(value: String, datatype: String): Unit
  
  def end(context: String): Unit
}