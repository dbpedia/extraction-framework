package org.dbpedia.validation.construct.tests.suites

import org.apache.spark.sql.SQLContext
import org.dbpedia.validation.construct.model.{TestCase, TestScore}
import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.validators.Validator

trait TestSuite extends Serializable {

  def test(path: String)(implicit SQLContext: SQLContext): Array[TestScore]

  val testCaseCollection: Array[TestCase]

  val triggerCollection: Array[Trigger]

  val validatorCollection: Array[Validator]

}
