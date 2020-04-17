package org.dbpedia.validation

import org.dbpedia.validation.TestCaseImpl._

object TriggerImpl {

  object TriggerType extends Enumeration {

    //MOVE Generic

    val BLANK, IRI, LITERAL, GENERIC_IRI, GENERIC_LITERAL, GENERIC_BLANK = Value
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

  case class LiteralTrigger(ID: TriggerID, datatype: String, testCases: Array[TestCase],
                            iri: String, label: String, comment: String) extends Trigger {

    override val TYPE: TriggerType.Value = TriggerType.LITERAL

    //    private val patter = s"\^\^<${datatype.replaceAll("/","\\/").replaceAll(".","\\.")}>$$".r.pattern

    private val matchPart = s"^^<$datatype>"

    override def isTriggered(nTriplePart: String): Boolean = {

      nTriplePart.endsWith(matchPart)
    }
  }

  /**
    * Triggers pretty xsd:String & rdf:langString
    * @param ID
    * @param testCases
    * @param iri
    * @param label
    * @param comment
    */
  case class DefaultLiteralTrigger(ID: TriggerID, testCases: Array[TestCase],
                                   iri: String, label: String, comment: String) extends Trigger {

    override val TYPE: TriggerType.Value = TriggerType.LITERAL

    // TODO regex only start and end with "
    private val pattern = "^\".*((\"@[a-zA-Z]*)|(\"))$".r.pattern

    override def isTriggered(nTriplePart: String): Boolean = {

      pattern.matcher(nTriplePart).matches()
    }
  }

  case class BlankNodeTrigger(ID: TriggerID, testCases: Array[TestCase],
                              iri: String, label: String, comment: String) extends Trigger {

    override val TYPE: TriggerType.Value = TriggerType.BLANK

    override def isTriggered(nTriplePart: String): Boolean = {false}
  }
}
