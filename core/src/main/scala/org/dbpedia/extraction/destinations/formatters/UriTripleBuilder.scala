package org.dbpedia.extraction.destinations.formatters

import java.net.{URI,URISyntaxException}
import UriPolicy._

abstract class UriTripleBuilder(policy: (URI, Int) => URI) extends TripleBuilder {
  
  protected val badUri = "BAD URI: "
  
  def subjectUri(subj: String) = uri(subj, SUBJECT)
  
  def predicateUri(pred: String) = uri(pred, PREDICATE)
  
  def objectUri(obj: String) = uri(obj, OBJECT)
  
  def uri(uri: String, pos: Int): Unit
  
  protected def parseUri(str: String, pos: Int): String = {
    try {
      var uri = new URI(str)
      uri = policy(uri, pos)
      uri.toString
    } catch {
      case usex: URISyntaxException =>
        badUri+usex.getMessage() 
    }
  }
}