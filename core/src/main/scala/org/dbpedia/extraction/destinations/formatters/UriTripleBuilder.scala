package org.dbpedia.extraction.destinations.formatters

import java.net.{URI,URISyntaxException}

abstract class UriTripleBuilder(iri: Boolean) extends TripleBuilder {
  
  protected val badUri = "BAD URI: "
  
  protected def parseUri(str: String): String = {
    try {
      val uri = new URI(str)
      if (iri) uri.toString else uri.toASCIIString
    } catch {
      case usex: URISyntaxException =>
        badUri+usex.getMessage() 
    }
  }
}