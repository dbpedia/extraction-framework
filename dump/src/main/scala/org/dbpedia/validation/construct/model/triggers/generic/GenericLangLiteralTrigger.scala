package org.dbpedia.validation.construct.model.triggers.generic

import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

/**
 * Combines trigger for literals with xsd:string or rdf:langString datatype
 *
 * @param ID id of the trigger
 * @param testCases set of test cases
 */
case class GenericLangLiteralTrigger(ID: TriggerID, testCases: Array[TestCase]) extends Trigger {

  override val TYPE: TriggerType.Value = TriggerType.LITERAL
  override val iri: TriggerIRI = "#GENERIC_LANG_LITERAL_TRIGGER"
  override val label: String = "GENERIC rdf:langString trigger"
  override val comment: String = "rdf language literals e.g. \"string\"@lang"

  // TODO regex only start and end with "
  private val pattern = "^\".*\"@[a-zA-Z0-9\\-]*$".r.pattern

  override def isTriggered(nTriplePart: String): Boolean = {

    pattern.matcher(nTriplePart).matches()
  }
}
