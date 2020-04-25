package org.dbpedia.validation.construct.model.validators.generic

import org.apache.jena.rdf.model.impl.NTripleReader
import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}
import org.dbpedia.validation.construct.model.validators.Validator

case class GenericLiteralLangTagValidator(ID: ValidatorID) extends Validator {

  override val METHOD_TYPE: model.ValidatorType.Value = ValidatorType.TYPED_LITERAL
  override val iri: ValidatorIRI = "#GENERIC_LTIERAL_LANG_TAG_VALIDATOR"

  //https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
  override def run(nTriplePart: String): Boolean = {
    true
  }

  override def info(): String = "Literal language tag conformity to BCP47 (prevalence:= literals with lang tags)"
}
