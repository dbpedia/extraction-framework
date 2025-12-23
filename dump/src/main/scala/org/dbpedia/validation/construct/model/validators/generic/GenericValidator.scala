package org.dbpedia.validation.construct.model.validators.generic

import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.validators.Validator
import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}

case class GenericValidator(ID: ValidatorID) extends Validator {

  override val iri: ValidatorIRI = "#GENERIC_VALIDATOR"

  override val METHOD_TYPE: model.ValidatorType.Value = ValidatorType.GENERIC

  override def run(nTriplePart: Construct): Boolean = true

  override def info(): String = "Missing validator: requires one ore more validators (always true)"
}
