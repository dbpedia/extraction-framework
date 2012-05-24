package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad

/**
 * Serializes statements.
 */
trait Formatter
{
  def header: String
  
  def footer: String
  
  def render(quad: Quad): String
}
