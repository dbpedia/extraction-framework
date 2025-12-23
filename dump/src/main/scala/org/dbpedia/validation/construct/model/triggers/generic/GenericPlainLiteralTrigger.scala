package org.dbpedia.validation.construct.model.triggers.generic

import java.util.regex.Pattern

import org.dbpedia.validation.construct.model
import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI}
import org.dbpedia.validation.construct.model.triggers.Trigger

case class GenericPlainLiteralTrigger(ID: TriggerID, testCases: Array[TestCase]) extends Trigger {

  override val TYPE: model.TriggerType.Value = model.TriggerType.LITERAL
  override val iri: TriggerIRI = "#GENERIC_PLAIN_LITERAL_TRIGGER"
  override val label: String = "GENERIC plain literal trigger"
  override val comment: String = "all plain literals"

    val pattern: Pattern = "\".*\"".r.pattern

  override def isTriggered(nTriplePart: String): Boolean = {
      if (pattern.matcher(nTriplePart).matches()) true
      else false
    }
}
