package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import java.net.URI

/**
 * TODO: add functionality - the comments could contain more useful info
 */
class TerseFormatter(render: Quad => String) extends Formatter
{
  def this(builder: => TripleBuilder) = 
    this(new TripleRenderer(builder))
  
  def this(quads: Boolean, turtle: Boolean, policy: (URI, Int) => URI = UriPolicy.identity) =
    this(new TerseBuilder(quads, turtle, policy))

  override def writeHeader(writer : Writer) = {
    writer.write("# started "+formatCurrentTimestamp+"\n")
  }
  
  override def writeFooter(writer : Writer) = {
    writer.write("# completed "+formatCurrentTimestamp+"\n")
  }
  
  override def write(writer : Writer, quad : Quad) = {
    writer.write(render(quad))
  }
}
