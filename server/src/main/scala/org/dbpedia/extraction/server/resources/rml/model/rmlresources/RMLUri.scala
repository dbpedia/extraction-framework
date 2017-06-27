package org.dbpedia.extraction.server.resources.rml.model.rmlresources

import org.apache.jena.util.URIref

/**
  * Represents a uri
  */
class RMLUri(uri: String) {

  override def toString = {
    RMLUri.encode(uri)
  }

  // returns a copy with an extended uri
  def extend(extension: String): RMLUri =
  {
    new RMLUri(uri + extension)
  }

}
object RMLUri {
  def encode(s : String) : String =
  {
    URIref.encode(s).replace(">", "%3E")
  }
}
