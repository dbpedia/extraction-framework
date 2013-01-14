package org.dbpedia.extraction.destinations.formatters

import UriPolicy._

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 1/12/13
 * Time: 9:15 PM
 * To change this template use File | Settings | File Templates.
 */
class RDFJSONFormatter(policies: Array[Policy] = null)
  extends TripleFormatter(() => new RDFJSONBuilder(policies)) {

  override def header = ""

  override def footer = ""

}
