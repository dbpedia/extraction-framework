package org.dbpedia.extraction.destinations.formatters

import UriPolicy._

/**
 * Serialize quads to RDF/JSON
 */
class RDFJSONFormatter(policies: Array[Policy] = null)
  extends TripleFormatter(() => new RDFJSONBuilder(policies)) {

  override def header = ""

  override def footer = ""

}
