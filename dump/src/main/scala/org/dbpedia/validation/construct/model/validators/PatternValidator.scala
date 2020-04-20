package org.dbpedia.validation.construct.model.validators

import java.util.regex.Pattern

import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

case class PatternValidator(ID: ValidatorID, iri: ValidatorIRI, patternString: String) extends Validator {

  val pattern: Pattern = patternString.r.pattern

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.PART_BASED

  override def run(nTriplePart: String): Boolean = {

    pattern.matcher(nTriplePart).matches()
  }

  override def info(): String = s"matches pattern $patternString"
}