package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import UriPolicy._

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 * 
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 * Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
class TriXFormatter(quads: Boolean, policies: Array[Policy] = null)
extends TripleFormatter(() => new TriXBuilder(quads, policies))
{
  override def header = "<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n"

  override def footer = "</TriX>\n"
}
