package org.dbpedia.validation.construct.model.validators

import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}


case class TypedLiteralValidator(ID: ValidatorID, iri: ValidatorIRI, patternString: String) extends Validator {

  private val pattern = patternString.r.pattern

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.TYPED_LITERAL

  override def run(nTriplePart: Construct): Boolean = {

    val lexicalForm = nTriplePart.self.trim.split("\"").dropRight(1).drop(1).mkString("")

    pattern.matcher(lexicalForm).matches()
  }

  override def info(): String = s"matching $patternString"
}
