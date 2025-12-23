package org.dbpedia.validation.construct.model.validators


import org.dbpedia.validation.construct.model.{Construct, ValidatorGroup, ValidatorID, ValidatorIRI, ValidatorType}

import scala.collection.immutable.HashSet

/**
 *
 * TODO use other set impl
 *
 * @param ID
 * @param vocabUrl
 * @param vocab
 */
case class VocabValidator(ID: ValidatorID, iri: ValidatorIRI, vocabUrl: String, vocab: HashSet[String],validatorGroup: ValidatorGroup.Value = ValidatorGroup.DEFAULT) extends Validator {

  override val METHOD_TYPE: ValidatorType.Value = ValidatorType.VOCAB_BASED
  override val VALIDATOR_GROUP: ValidatorGroup.Value = validatorGroup

  override def run(nTriplePart: Construct): Boolean = {
    VALIDATOR_GROUP match {
      case ValidatorGroup.RIGHT => nTriplePart.right match {
        // TODO: maybe we need to rename "value"
        case Some(value) => vocab.contains(value)
        case None => false
      }
      case ValidatorGroup.LEFT => nTriplePart.left match {
        case Some(value) => vocab.contains(value)
        case None => false
      }
      case _ => vocab.contains(nTriplePart.self)
    }
  }

  override def info(): String = s"one of vocab $vocabUrl"
}
