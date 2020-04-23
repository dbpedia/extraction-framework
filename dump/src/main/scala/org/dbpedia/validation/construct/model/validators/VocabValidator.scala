package org.dbpedia.validation.construct.model.validators


import org.dbpedia.validation.construct.model.{ValidatorID, ValidatorIRI, ValidatorType}

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

  override def run(nTriplePart: String): Boolean = {

    val bool = vocab.contains(nTriplePart)
//    if (! bool ) println(vocabUrl,nTriplePart)
    bool
  }

  override def info(): String = s"one of vocab $vocabUrl"
}
