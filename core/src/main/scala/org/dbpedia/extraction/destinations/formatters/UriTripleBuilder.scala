package org.dbpedia.extraction.destinations.formatters

import java.net.{URI,URISyntaxException}
import UriPolicy._

/**
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 * Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
abstract class UriTripleBuilder(policies: Array[Policy] = null) extends TripleBuilder {
  
  protected val BadUri = "BAD URI: "
  
  def subjectUri(subj: String) = uri(subj, SUBJECT)
  
  def predicateUri(pred: String) = uri(pred, PREDICATE)
  
  def objectUri(obj: String) = uri(obj, OBJECT)
  
  def uri(uri: String, pos: Int): Unit
  
  protected def parseUri(str: String, pos: Int): String = {
    if (str == null) return BadUri+str
    try {
      var uri = new URI(str)
      if (! uri.isAbsolute()) return BadUri+"not absolute: "+str
      if (policies != null) uri = policies(pos)(uri)
      uri.toString
    } catch {
      case usex: URISyntaxException =>
        BadUri+usex.getMessage() 
    }
  }
}