package org.dbpedia.validation

import org.dbpedia.validation.TestCaseImpl._
import org.dbpedia.validation.TriggerImpl.{Trigger, TriggerID}

object TestSuiteImpl {

  case class TestSuite(
                         triggerCollection: Array[Trigger],

                         testApproachCollection: Array[TestApproach],

                         maxTestApproachID: TestApproachID,

                         maxTriggerID: TriggerID,

                         maxTestCaseID: TestCaseID
                      )
}
