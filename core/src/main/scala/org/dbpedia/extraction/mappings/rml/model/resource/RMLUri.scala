package org.dbpedia.extraction.mappings.rml.model.resource

import org.apache.jena.iri.{IRI, IRIFactory}

/**
  * Represents a uri
  */
case class RMLUri(uri: String) {

  override def toString = {
    RMLUri.encode(uri)
  }

  def getIRI: IRI = {
    IRIFactory.iriImplementation.create(uri)
  }

  // returns a copy with an extended uri
  def extend(extension: String): RMLUri = {
    RMLUri(uri + extension)
  }

}

object RMLUri {

  def encode(s: String): String = {
    val encoded = IRIFactory.iriImplementation.construct(s)
    val toString = encoded.toString
    toString
  }

  val SIMPLEPROPERTYMAPPING = "SimplePropertyMapping"

  val CONSTANTMAPPING = "ConstantMapping"

  val LATITUDEMAPPING = "LatitudeMapping"

  val LONGITUDEMAPPING = "LongitudeMapping"

  val STARTDATEMAPPING = "StartDateMapping"

  val ENDDATEMAPPING = "EndDateMapping"

  val CONDITIONALMAPPING = "ConditionalMapping"

  val INTERMEDIATEMAPPING = "IntermediateMapping"
}
