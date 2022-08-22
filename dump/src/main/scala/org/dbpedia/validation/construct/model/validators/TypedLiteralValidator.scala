package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{Construct, ValidatorGroup, ValidatorID, ValidatorIRI, ValidatorType}


case class TypedLiteralValidator(ID: ValidatorID, iri: ValidatorIRI, patternString: String, validatorGroup: ValidatorGroup.Value = ValidatorGroup.DEFAULT) extends Validator {

  private val pattern = patternString.r.pattern

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.TYPED_LITERAL
  override val VALIDATOR_GROUP: ValidatorGroup.Value = validatorGroup

  override def run(nTriplePart: Construct): Boolean = {
    val lexicalForm = VALIDATOR_GROUP match {
      case ValidatorGroup.RIGHT => nTriplePart.right match {
        // TODO: 1) maybe we need to rename "value"
        //       2) discuss what to do if we want to check the value that doesn't exist on
        //          the left or right side, at the moment we only return false in these cases
        case Some(value) => value.trim.split("\"").dropRight(1).drop(1).mkString("")
        case None => return false
      }
      case ValidatorGroup.LEFT => nTriplePart.left match {
        case Some(value) => value.trim.split("\"").dropRight(1).drop(1).mkString("")
        case None => return false
      }
      case _ => nTriplePart.self.trim.split("\"").dropRight(1).drop(1).mkString("")
    }
    pattern.matcher(lexicalForm).matches()
  }

  override def info(): String = s"matching $patternString"
}
