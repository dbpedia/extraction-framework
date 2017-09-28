package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.transform.Quad
/**
  Will be thrown by WriterDestination to signal failed Quads
  (i.e. Bad Iri)
 */
class BadQuadException(msg : String) extends Exception(msg) {
  def this(msg: String, quad: Quad) = this(msg+" at Quad: s:'" + quad.subject + "', p:'" + quad.predicate + "', o:'" + quad.value +"'")
}
