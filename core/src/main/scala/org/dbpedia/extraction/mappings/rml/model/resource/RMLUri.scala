package org.dbpedia.extraction.mappings.rml.model.resource

import org.apache.jena.iri.{IRI, IRIFactory}
import org.apache.jena.util.URIref
import org.dbpedia.extraction.util.UriUtils

/**
  * Represents a uri
  */
class RMLUri(uri: String) {

  override def toString = {
    RMLUri.encode(uri)
  }

  def getIRI : IRI = {
    IRIFactory.iriImplementation.create(uri)
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
    val encoded = IRIFactory.iriImplementation.construct(s)
    val uri = encoded.toURI
    val displayString = encoded.toDisplayString
    val errors = encoded.violations(true).hasNext
    val toString = encoded.toString
    toString
  }

  val SIMPLEPROPERTYMAPPING = "SimplePropertyMapping"

  val CONSTANTMAPPING = "ConstantMapping"

  val LATITUDEMAPPING = "LatitudeMapping"

  val LONGITUDEMAPPING = "LongitudeMapping"
}
