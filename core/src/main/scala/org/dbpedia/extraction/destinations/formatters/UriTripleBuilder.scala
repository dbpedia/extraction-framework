package org.dbpedia.extraction.destinations.formatters

import java.net.{URI,URISyntaxException}

abstract class UriTripleBuilder(iri: Boolean) extends TripleBuilder {
  
  private var badUris = 0
  
  protected def badUri(badUris: Int): Unit
  
  protected def parseUri(str: String): String = {
    try {
      val uri = new URI(str)
      if (iri) uri.toString else uri.toASCIIString
    } catch {
      case usex: URISyntaxException =>
        badUris += 1
        badUri(badUris)
        "BAD URI: "+usex.getMessage() 
    }
  }
}