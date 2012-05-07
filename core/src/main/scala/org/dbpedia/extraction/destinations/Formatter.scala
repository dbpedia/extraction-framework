package org.dbpedia.extraction.destinations

import java.io.Writer

/**
 * Serializes statements.
 */
trait Formatter
{
  /**
   * Recommended suffix for files written in this format. Starts with a letter, not with a dot. 
   */
  val fileSuffix: String
  
  def writeHeader(writer : Writer) : Unit
  
  def writeFooter(writer : Writer) : Unit
  
  def write(writer : Writer, quad : Quad) : Unit
}
