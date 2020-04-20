package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

/**
 * TODO
 * @param ID
 */
case class GenericLiteralValidator(ID: ValidatorID) extends Validator {

  override val iri: ValidatorIRI = "#GENERIC_LITERAL_VALIDATOR"

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.GENERIC

  override def run(nTriplePart: String): Boolean = ???

  override def info(): String = ???
}

