package org.dbpedia.validation.construct.model.validators

import java.util.regex.Pattern
import org.dbpedia.validation.construct.model.{Construct, ValidatorGroup, ValidatorID, ValidatorIRI, ValidatorType}

case class PatternValidator(ID: ValidatorID, iri: ValidatorIRI, patternString: String, validatorGroup: ValidatorGroup.Value = ValidatorGroup.DEFAULT) extends Validator {

  val pattern: Pattern = patternString.r.pattern

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.PART_BASED
  override val VALIDATOR_GROUP: ValidatorGroup.Value = validatorGroup

  override def run(nTriplePart: Construct): Boolean = {
    VALIDATOR_GROUP match {
      case ValidatorGroup.RIGHT => nTriplePart.right match {
      // TODO: maybe we need to rename "value"
        case Some(value) => pattern.matcher(value).matches()
        case None => false
      }
      case ValidatorGroup.LEFT => nTriplePart.left match {
        case Some(value) => pattern.matcher(value).matches()
        case None => false
      }
      case _ => pattern.matcher(nTriplePart.self).matches()
    }
  }

  override def info(): String = s"matches pattern $patternString"
}