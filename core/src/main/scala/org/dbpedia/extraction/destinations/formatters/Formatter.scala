package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.Quad

/**
 * Serializes statements.
 */
trait Formatter
{
  def writeHeader(writer : Writer) : Unit
  
  def writeFooter(writer : Writer) : Unit
  
  def write(writer : Writer, quad : Quad) : Unit
}
