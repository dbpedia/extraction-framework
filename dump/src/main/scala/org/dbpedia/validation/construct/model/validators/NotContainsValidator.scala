package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

case class NotContainsValidator(ID: ValidatorID, iri: ValidatorIRI, sequence: String) extends Validator {

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.PART_BASED

  override def run(nTriplePart: String): Boolean = {

  ! nTriplePart.contains(sequence)
}

  override def info(): String = s"does not contain $sequence"
}
