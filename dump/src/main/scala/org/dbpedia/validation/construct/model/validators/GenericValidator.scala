package org.dbpedia.validation.construct.model.validators
import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

case class GenericValidator(ID: ValidatorID) extends Validator {

  override val iri: ValidatorIRI = "#GENERIC_VALIDATOR"

  override val METHOD_TYPE: model.ValidatorType.Value = ValidatorType.GENERIC

  override def run(nTriplePart: String): Boolean = true

  override def info(): String = "missing validator: requires one ore more validators (always true)"
}
