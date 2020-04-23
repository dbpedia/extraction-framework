package org.dbpedia.validation.construct.model.triggers

import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

// TODO create only a literal trigger
case class TypedLiteralTrigger(ID: TriggerID, datatype: String, testCases: Array[TestCase],
                               iri: TriggerIRI, label: String, comment: String) extends Trigger {

  override val TYPE: TriggerType.Value = TriggerType.LITERAL

  //    private val patter = s"\^\^<${datatype.replaceAll("/","\\/").replaceAll(".","\\.")}>$$".r.pattern

  private val matchPart = s"^^<$datatype>"

  override def isTriggered(nTriplePart: String): Boolean = {

    nTriplePart.endsWith(matchPart)
  }
}