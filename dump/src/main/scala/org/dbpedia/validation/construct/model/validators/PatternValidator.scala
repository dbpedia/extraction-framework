package org.dbpedia.validation.construct.model.validators

import java.util.regex.Pattern
import org.dbpedia.validation.construct.model.{Construct, ValidatorGroup, ValidatorID, ValidatorIRI, ValidatorType}

case class PatternValidator(ID: ValidatorID, iri: ValidatorIRI, patternString: String, validatorGroup: ValidatorGroup.Value = ValidatorGroup.DEFAULT) extends Validator {

  val pattern: Pattern = patternString.r.pattern

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.PART_BASED
  override val VALIDATOR_GROUP: ValidatorGroup.Value = validatorGroup

  override def run(nTriplePart: Construct): Boolean = {
    val result: Boolean = VALIDATOR_GROUP match {
        //TODO: make pattern matching for nTriplePart.right/left because it is optional
      case ValidatorGroup.RIGHT => pattern.matcher(nTriplePart.right.get).matches()
      case ValidatorGroup.LEFT => pattern.matcher(nTriplePart.left.get).matches()
      case _ => pattern.matcher(nTriplePart.self).matches()
    }
    result
  }

  override def info(): String = s"matches pattern $patternString"
}