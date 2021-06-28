package org.dbpedia.validation.construct.model.validators.generic

import org.apache.jena.riot.system.IRIResolver
import org.dbpedia.validation.construct.model.validators.Validator
import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}

case class GenericIRIValidator(ID: ValidatorID) extends Validator {

  override val iri: ValidatorIRI = "#GENERIC_IRI_VALIDATOR"

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.GENERIC

  override def run(nTriplePart: Construct): Boolean = {

    !IRIResolver.checkIRI(nTriplePart.self)
  }

  override def info(): String = "IRI Validation with Apache Jena IRI parser (prevalence:= all IRIs)"
}
