package org.dbpedia.extraction.server.resources.rml.model.rmlresources

/**
  * Represents a uri
  */
class RMLUri(uri: String) {

  override def toString() = {
    encode(uri)
  }

  // returns a copy with an extended uri
  def extend(extension: String): RMLUri =
  {
    new RMLUri(uri + extension)
  }

  private def encode(s : String) : String =
  {
    s.replaceAll(" ", "%20")
  }

}
