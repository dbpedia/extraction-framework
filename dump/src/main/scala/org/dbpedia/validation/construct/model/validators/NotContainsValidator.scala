package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{Construct, ValidatorGroup, ValidatorID, ValidatorIRI, ValidatorType}

case class NotContainsValidator(ID: ValidatorID, iri: ValidatorIRI, sequence: String, validatorGroup: ValidatorGroup.Value = ValidatorGroup.DEFAULT) extends Validator {

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.PART_BASED
  override val VALIDATOR_GROUP: ValidatorGroup.Value = validatorGroup

  override def run(nTriplePart: Construct): Boolean = {
    VALIDATOR_GROUP match {
      case ValidatorGroup.RIGHT => nTriplePart.right match {
        // TODO: maybe we need to rename "value"
        case Some(value) => !value.contains(sequence)
        case None => false
      }
      case ValidatorGroup.LEFT => nTriplePart.left match {
        case Some(value) => !value.contains(sequence)
        case None => false
      }
      case _ => !nTriplePart.self.contains(sequence)
    }
  }

  override def info(): String = s"does not contain $sequence"
}
