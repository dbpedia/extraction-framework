package org.dbpedia.validation.construct.model.triggers

import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

trait Trigger {

  val ID: TriggerID

  val TYPE: TriggerType.Value

  val iri: TriggerIRI

  val label: String

  val comment: String

  val testCases: Array[TestCase]

  def isTriggered(nTriplePart: String) : Boolean
}
