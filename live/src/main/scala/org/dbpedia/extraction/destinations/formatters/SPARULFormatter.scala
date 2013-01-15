package org.dbpedia.extraction.destinations.formatters

import UriPolicy._

/**
 * TODO: add functionality - the comments could contain more useful info
 *
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 *                 Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
class SPARULFormatter(insOrDel: Boolean, graph: String, policies: Array[Policy] = null)
  extends TripleFormatter(() => new SPARULBuilder(policies))  {
  override def header = (if (insOrDel) "INSERT DATA INTO" else "DELETE DATA FROM") + " <" + graph + "> {\n"

  override def footer = " } "
}
