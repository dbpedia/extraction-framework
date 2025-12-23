package org.dbpedia.validation.construct.model.triggers

import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

case class BlankNodeTrigger(ID: TriggerID, testCases: Array[TestCase],
                            iri: TriggerIRI, label: String, comment: String) extends Trigger {

  override val TYPE: TriggerType.Value = TriggerType.BLANK_NODE

  override def isTriggered(nTriplePart: String): Boolean = {false}
}