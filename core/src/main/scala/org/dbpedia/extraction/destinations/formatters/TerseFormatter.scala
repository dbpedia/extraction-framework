package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import UriPolicy._

/**
 * TODO: add functionality - the comments could contain more useful info
 */
class TerseFormatter(renderer: Quad => String) extends Formatter
{
  def this(builder: => TripleBuilder) = 
    this(new TripleRenderer(builder))
  
  def this(quads: Boolean, turtle: Boolean, policy: Policy = identity) =
    this(new TerseBuilder(quads, turtle, policy))

  override def header = "# started "+formatCurrentTimestamp+"\n"
  
  override def footer = "# completed "+formatCurrentTimestamp+"\n"
  
  override def render(quad: Quad) = renderer(quad)
}
