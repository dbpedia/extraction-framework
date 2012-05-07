package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}
import scala.xml.Utility.escape
import java.net.URI

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(iri: Boolean, quads: Boolean, header: String = null) extends Formatter
{
  
  override val fileSuffix = {
    '.' + (if (iri) "iri" else "uri") + '.' + (if (quads) "quads" else "triples") + ".trix"
  }

  override def writeHeader(writer : Writer): Unit = {
    if (header != null) writer.write(header)
    writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
  }

  override def writeFooter(writer : Writer): Unit = {
    writer.write("</TriX>\n")
  }

  override def write(writer : Writer, quad : Quad) : Unit = {
    writer.write(new TriXBuilder(iri, quads).render(quad))
  }
}

object TriXFormatter {
  
  def all = for (iri <- 0 to 1; quads <- 0 to 1) yield new TriXFormatter(iri == 0, quads == 0)
  
  def QuadsIris(header: String = null) = new TriXFormatter(true, true, header)
  def QuadsUris(header: String = null) = new TriXFormatter(false, true, header)
  def TriplesIris(header: String = null) = new TriXFormatter(true, false, header)
  def TriplesUris(header: String = null) = new TriXFormatter(false, false, header)
}