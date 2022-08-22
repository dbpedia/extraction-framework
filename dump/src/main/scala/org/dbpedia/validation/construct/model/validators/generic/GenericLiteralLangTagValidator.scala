package org.dbpedia.validation.construct.model.validators.generic

import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}
import org.dbpedia.validation.construct.model.validators.Validator

/**
 * TODO: use jena strict?
 * @param ID
 */
case class GenericLiteralLangTagValidator(ID: ValidatorID) extends Validator {

  override val METHOD_TYPE: model.ValidatorType.Value = ValidatorType.TYPED_LITERAL
  override val iri: ValidatorIRI = "#GENERIC_LTIERAL_LANG_TAG_VALIDATOR"

  private val pattern = ".*@[a-zA-Z]+(-[a-zA-Z0-9]+)*$".r.pattern

  override def run(nTriplePart: Construct): Boolean = {

    pattern.matcher(nTriplePart.self).matches()
  }

  override def info(): String = "Literal language tag conformity to BCP47 (prevalence:= literals with lang tags)"
}
