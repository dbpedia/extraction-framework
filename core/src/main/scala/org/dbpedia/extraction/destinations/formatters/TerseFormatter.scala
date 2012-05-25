package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import UriPolicy._

/**
 * TODO: add functionality - the comments could contain more useful info
 */
class TerseFormatter(quads: Boolean, turtle: Boolean, policy: Policy = identity)
extends TripleFormatter(() => new TerseBuilder(quads, turtle, policy))
{
  override def header = "# started "+formatCurrentTimestamp+"\n"
  
  override def footer = "# completed "+formatCurrentTimestamp+"\n"
}
