package org.dbpedia.validation.construct.model.triggers.generic

import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

case class GenericIRITrigger(ID: TriggerID, testCases: Array[TestCase]) extends Trigger {

  val iri: TriggerIRI = "#GENERIC_IRI_TRIGGER"
  val label: String = "GENERIC iri trigger"
  val comment: String = "all IRIs"

  override val TYPE: TriggerType.Value = TriggerType.IRI

  override def isTriggered(nTriplePart: String): Boolean = true

}
