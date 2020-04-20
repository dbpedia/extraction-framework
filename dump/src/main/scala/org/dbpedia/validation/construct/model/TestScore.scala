package org.dbpedia.validation.construct.model

// TODO total, covered, prevalenceOfTriggers, successOfValidator
case class TestScore(total: Long, covered: Long, valid: Long, prevalenceOfTriggers: Array[Long], errorsOfTestCases: Array[Long] ) {

  def +(testReport: TestScore): TestScore = {

    TestScore(
      total + testReport.total,
      covered + testReport.covered,
      valid + testReport.valid,
      prevalenceOfTriggers.zip(testReport.prevalenceOfTriggers).map { case (x, y) => x + y },
      errorsOfTestCases.zip(testReport.errorsOfTestCases).map { case (x, y) => x + y }
    )
  }

  lazy val coverage: Float = covered.toFloat/total.toFloat

  lazy val errorsPerConstruct: Float = {
    errorsOfTestCases.sum.toFloat / total
  }

  lazy val errorsPerCovered: Float = {
    (covered - valid)/covered.toFloat
  }
}
