package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.Quad
import java.net.URI

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(render: Quad => String) extends Formatter
{
  def this(builder: => TripleBuilder) =
    this(new TripleRenderer(builder))

  def this(quads: Boolean, policy: (URI, Int) => URI = UriPolicy.identity) =
    this(new TriXBuilder(quads, policy))

  override def writeHeader(writer : Writer): Unit = {
    writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
  }

  override def writeFooter(writer : Writer): Unit = {
    writer.write("</TriX>\n")
  }

  override def write(writer : Writer, quad : Quad) : Unit = {
    writer.write(render(quad))
  }
}
