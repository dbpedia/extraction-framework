package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import UriPolicy._

/**
 * TODO: add functionality - the comments could contain more useful info
 * 
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 * Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
class TerseFormatter(quads: Boolean, turtle: Boolean, policies: Array[Policy] = null)
extends TripleFormatter(() => new TerseBuilder(quads, turtle, policies))
{
  override def header = "# started "+formatCurrentTimestamp+"\n"
  
  override def footer = "# completed "+formatCurrentTimestamp+"\n"
}
