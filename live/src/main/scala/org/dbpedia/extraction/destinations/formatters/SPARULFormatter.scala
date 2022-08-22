package org.dbpedia.extraction.destinations.formatters

import UriPolicy._

/**
 * SPARUL triple blocks are Turtle-compatible so we modify only the heafer / footer
 * and use a Turtle builder for the rest
 *
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 *                 Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
class SPARULFormatter(insOrDel: Boolean, graph: String, policies: Array[Policy] = null)
  extends TripleFormatter(() =>new TerseBuilder(false, true, policies))  {
  override def header = (if (insOrDel) "INSERT DATA INTO" else "DELETE DATA FROM") + " <" + graph + "> {\n"

  override def footer = " } "
}
