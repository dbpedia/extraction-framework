package org.dbpedia.validation.construct.model.validators.generic

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.apache.jena.riot.{RIOT, RiotException}
import org.apache.jena.riot.lang.LangNTriples
import org.apache.jena.riot.system.{ErrorHandlerFactory, IRIResolver, ParserProfileStd, PrefixMapFactory, RiotLib}
import org.apache.jena.riot.tokens.TokenizerFactory
import org.dbpedia.validation.construct.model.validators.Validator
import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}

/**
 * TODO
 *
 * @param ID
 */
case class GenericLiteralValidator(ID: ValidatorID) extends Validator {

  override val iri: ValidatorIRI = "#GENERIC_LITERAL_VALIDATOR"

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.GENERIC

  override def run(nTriplePart: Construct): Boolean = {

    val triple = "<> <> "+nTriplePart.self+" ."
    val profile = {
      new ParserProfileStd(RiotLib.factoryRDF, ErrorHandlerFactory.errorHandlerStrict,
        IRIResolver.create, PrefixMapFactory.createForInput,
        RIOT.getContext.copy, true, true)
    }

    val tokenizer =
      TokenizerFactory.makeTokenizerUTF8(new ByteArrayInputStream(triple.getBytes(StandardCharsets.UTF_8)))

    try { new LangNTriples(tokenizer, profile, null).next(); true }
    catch { case _: RiotException => false }
  }

  override def info(): String = "Literal validation with Apache Jena literal parser (prevalence:= all literals)"
}
