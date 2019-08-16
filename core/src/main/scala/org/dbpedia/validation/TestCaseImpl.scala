package org.dbpedia.validation
import java.util.regex.Pattern

import org.apache.jena.riot.system.IRIResolver
import org.dbpedia.validation.TriggerImpl.TriggerID

import scala.collection.immutable.HashSet

object TestCaseImpl {

  type TestCaseID = Int

  case class TestCase(ID: TestCaseID, triggerID: TriggerID, testAproachID: TestApproachID)

  object TestApproachType extends Enumeration {

    val PATTERN_BASED, VOCAB_BASED, PART_BASED, GENERIC = Value
  }

  type TestApproachID = Int

  trait TestApproach {

    val ID: TestApproachID

    val METHOD_TYPE: TestApproachType.Value

    /**
      * Run TestCase against a NTriplePart ( one of {s,p,o} )
      * @param nTriplePart part of an NTripleRow { row.trim.split(" ",3) }
      * @return true if test successful
      */
    def run(nTriplePart: String ): Boolean

    def info(): String

    override def toString: String = info()
  }

  case class PatternTestApproach(ID: TestApproachID, patternString: String) extends TestApproach {

    val pattern: Pattern = patternString.r.pattern

    override val METHOD_TYPE: TestApproachType.Value = TestApproachType.PART_BASED

    override def run(nTriplePart: String): Boolean = {

      pattern.matcher(nTriplePart).matches()
    }

    override def info(): String = s"matches pattern $patternString"
  }

  case class VocabTestApproach(ID: TestApproachID, vocabUrl: String, vocab: HashSet[String]) extends TestApproach {

    override val METHOD_TYPE: TestApproachType.Value = TestApproachType.VOCAB_BASED

    override def run(nTriplePart: String): Boolean = {

      vocab.contains(nTriplePart)
    }

    override def info(): String = s"one of vocab $vocabUrl"
  }

  case class NotContainsTestApproach(ID: TestApproachID, sequence: String) extends TestApproach {

    override val METHOD_TYPE: TestApproachType.Value = TestApproachType.PART_BASED

    override def run(nTriplePart: String): Boolean = {

      ! nTriplePart.contains(sequence)
    }

    override def info(): String = s"does not contain $sequence"
  }

  case class GenericIRITestApproach(ID: TestApproachID) extends TestApproach {

    override val METHOD_TYPE: TestApproachType.Value = TestApproachType.GENERIC

    override def run(nTriplePart: String): Boolean = {

      ! IRIResolver.checkIRI(nTriplePart)
    }

    override def info(): String = "parsed successfully"
  }
}
