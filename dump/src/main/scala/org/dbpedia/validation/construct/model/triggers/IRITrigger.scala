package org.dbpedia.validation.construct.model.triggers

import org.dbpedia.validation.construct.model.{TestCase, TriggerID, TriggerIRI, TriggerType}

case class IRITrigger(ID: TriggerID, patternStrings: Array[String], testCases: Array[TestCase],
                      iri: TriggerIRI, label: String, comment: String) extends Trigger {

//  private val patterns = patternStrings.map( patternString => s"$patternString.*".r.pattern)
  private val patterns = patternStrings.map( patternString => patternString.r.pattern)

  override val TYPE: TriggerType.Value = TriggerType.IRI

  override def isTriggered(nTriplePart: String): Boolean = {

    var matched = false

    patterns.foreach( pattern => if(pattern.matcher(nTriplePart).matches()) matched = true )

    matched
  }
}