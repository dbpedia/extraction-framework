package org.dbpedia.validation.construct.model

// TODO total, covered, prevalenceOfTriggers, successOfValidator
case class TestScore(
                      total: Long,
                      coveredGeneric: Long, coveredCustom: Long,
                      validGeneric: Long, validCustom: Long,
                      prevalenceOfTriggers: Array[Long],
                      errorsOfTestCases: Array[Long]
                    ) {

  def +(testReport: TestScore): TestScore = {

    TestScore(
      total + testReport.total,
      coveredGeneric + testReport.coveredGeneric,
      coveredCustom + testReport.coveredCustom,
      validGeneric + testReport.validGeneric,
      validCustom + testReport.validCustom,
      prevalenceOfTriggers.zip(testReport.prevalenceOfTriggers).map { case (x, y) => x + y },
      errorsOfTestCases.zip(testReport.errorsOfTestCases).map { case (x, y) => x + y }
    )
  }

  lazy val coverage: Float = 0.0f // TODO covered.toFloat/total.toFloat

  lazy val errorsPerConstruct: Float = {
    //    errorsOfTestCases.sum.toFloat / total
    // TODO
    0.0f
  }


  lazy val errorsPerCovered: Float = {
    //    (covered - validCustom)/covered.toFloat
    // TODO
    0.0f
  }
}
