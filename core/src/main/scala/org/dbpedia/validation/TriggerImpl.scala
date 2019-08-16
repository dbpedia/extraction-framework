package org.dbpedia.validation

import org.dbpedia.validation.TestCaseImpl._

object TriggerImpl {

  object TriggerType extends Enumeration {

    val BLANK, IRI, LITERAL = Value
  }

  type TriggerID = Int

  trait Trigger {

    val ID: TriggerID

    val TYPE: TriggerType.Value

    val iri: String

    val label: String

    val comment: String

    val testCases: Array[TestCase]

    def isTriggered(nTriplePart: String) : Boolean
  }

  case class IRITrigger(ID: TriggerID, patternStrings: Array[String], testCases: Array[TestCase],
                        iri: String, label: String, comment: String) extends Trigger {

    private val patterns = patternStrings.map( patternString => s"$patternString.*".r.pattern)

    override val TYPE: TriggerType.Value = TriggerType.IRI

    override def isTriggered(nTriplePart: String): Boolean = {

      var matched = false

      patterns.foreach( pattern => if(pattern.matcher(nTriplePart).matches()) matched = true )

      matched
    }
  }

  case class LiteralTrigger(ID: TriggerID, testCases: Array[TestCase],
                            iri: String, label: String, comment: String) extends Trigger {

    override val TYPE: TriggerType.Value = TriggerType.LITERAL

    override def isTriggered(nTriplePart: String): Boolean = {false}
  }

  case class BlankNodeTrigger(ID: TriggerID, testCases: Array[TestCase],
                              iri: String, label: String, comment: String) extends Trigger {

    override val TYPE: TriggerType.Value = TriggerType.BLANK

    override def isTriggered(nTriplePart: String): Boolean = {false}
  }
}
