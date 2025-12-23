package org.dbpedia.validation.construct.model.validators.generic

import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI}
import org.dbpedia.validation.construct.model.validators.Validator

case class GenericRdfLangStringValidator(ID: ValidatorID) extends Validator {

  override val METHOD_TYPE: model.ValidatorType.Value = model.ValidatorType.TYPED_LITERAL
  override val iri: ValidatorIRI = "#GENERIC_RDF_LANG_STRING_VALIDATOR"

  override def run(nTriplePart: Construct): Boolean = {
    !nTriplePart.self.endsWith("<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>")
  }

  override def info(): String = "rdf:langString is an implicit type and must never be serialized. (prevalence := all typed literals)"
}
