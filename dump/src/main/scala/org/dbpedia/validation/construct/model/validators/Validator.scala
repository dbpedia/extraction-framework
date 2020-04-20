package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

trait Validator {

  val ID: ValidatorID

  val METHOD_TYPE: ValidatorType.Value

  val iri: ValidatorIRI

  /**
   * Run TestCase against a NTriplePart ( one of {s,p,o} )
   * @param nTriplePart part of an NTripleRow { row.trim.split(" ",3) }
   * @return true if test successful
   */
  def run(nTriplePart: String): Boolean

  def info(): String

  override def toString: String = info()
}