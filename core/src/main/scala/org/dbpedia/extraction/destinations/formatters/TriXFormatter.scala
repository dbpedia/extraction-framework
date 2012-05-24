package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import UriPolicy._

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(renderer: Quad => String) extends Formatter
{
  def this(builder: => TripleBuilder) =
    this(new TripleRenderer(builder))

  def this(quads: Boolean, policy: Policy = identity) =
    this(new TriXBuilder(quads, policy))

  override def header = "<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n"

  override def footer = "</TriX>\n"

  override def render(quad: Quad) = renderer(quad)
}
