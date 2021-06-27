package org.dbpedia.validation.construct.model.validators


import org.dbpedia.validation.construct.model.{Construct, ValidatorID, ValidatorIRI, ValidatorType}

import scala.collection.immutable.HashSet

/**
 *
 * TODO use other set impl
 *
 * @param ID
 * @param vocabUrl
 * @param vocab
 */
case class VocabValidator(ID: ValidatorID, iri: ValidatorIRI, vocabUrl: String, vocab: HashSet[String]) extends Validator {

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.VOCAB_BASED

  override def run(nTriplePart: Construct): Boolean = {

    val bool = vocab.contains(nTriplePart.self)
//    if (! bool ) println(vocabUrl,nTriplePart)
    bool
  }

  override def info(): String = s"one of vocab $vocabUrl"
}
