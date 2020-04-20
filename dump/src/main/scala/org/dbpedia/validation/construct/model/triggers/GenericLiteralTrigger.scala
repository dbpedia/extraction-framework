package org.dbpedia.validation.construct.model.triggers

import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

/**
 * Combines trigger for literals with xsd:string or rdf:langString datatype
 *
 * @param ID id of the trigger
 * @param testCases set of test cases
 */
case class GenericLiteralTrigger(ID: TriggerID, testCases: Array[TestCase]) extends Trigger {

  override val iri: TriggerIRI = "#GENERIC_LITERAL_TRIGGER"

  override val label: String = "xsd:string or rdf:langString"

  override val comment: String = "xsd:string or rdf:langString"

  override val TYPE: TriggerType.Value = TriggerType.LITERAL

  // TODO regex only start and end with "
  private val pattern = "^\".*((\"@[a-zA-Z]*)|(\"))$".r.pattern

  override def isTriggered(nTriplePart: String): Boolean = {

    pattern.matcher(nTriplePart).matches()
  }
}