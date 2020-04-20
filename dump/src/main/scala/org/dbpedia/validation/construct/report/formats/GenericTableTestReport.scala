package org.dbpedia.validation.construct.report.formats

import org.dbpedia.validation.construct.model.TestScore
import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.validators.Validator

import scala.collection.mutable.ArrayBuffer

object GenericTableTestReport {

  object Tabulator {

    def format(table: Seq[Seq[Any]]): String = table match {
      case Seq() => ""
      case _ =>
        val sizes = for (row <- table) yield for (cell <- row) yield if (cell == null) 0 else cell.toString.length
        val colSizes = for (col <- sizes.transpose) yield col.max
        val rows = for (row <- table) yield formatRow(row, colSizes)
        formatRows(rowSeparator(colSizes), rows)
    }

    def formatRows(rowSeparator: String, rows: Seq[String]): String = (
      rowSeparator ::
        rows.head ::
        rowSeparator ::
        rows.tail.toList :::
        rowSeparator ::
        List()).mkString("\n")

    def formatRow(row: Seq[Any], colSizes: Seq[Int]): String = {
      val cells = for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s").format(item)
      cells.mkString("|", "|", "|")
    }

    def rowSeparator(colSizes: Seq[Int]): String = colSizes map { "-" * _ } mkString("+", "+", "+")
  }

  def build(label : String,
            testReport: TestScore,
            triggerCollection: Array[Trigger],
            testApproachCollection: Array[Validator] ) : Unit = {

    println()
    print(s"$label")
    print(s" -- Coverage: ${testReport.coverage} " +
      s"(${testReport.coverage} triggered of ${testReport.total} total)")

    val errorRatesBuffer = ArrayBuffer[Float]()
    val testCaseSerializationBuffer = ArrayBuffer[Seq[String]]()

    testCaseSerializationBuffer.append(
      Seq("Trigger", "Test Approach", "Prevalence", "Errors", "Error Rate")
    )

    triggerCollection.foreach(trigger => {

      if (trigger.testCases.length == 0) {

        // Does not increase the error rate
        testCaseSerializationBuffer.append(
          Seq(
            " " + trigger.label + " { id: " + trigger.iri + " } ",
            " missing validator ",
            testReport.prevalenceOfTriggers(trigger.ID).toString,
            testReport.prevalenceOfTriggers(trigger.ID).toString,
            "0.0"
          )
        )
      }

      trigger.testCases.foreach(testCase => {

        val prevalence = testReport.prevalenceOfTriggers(trigger.ID)
        val errors = testReport.errorsOfTestCases(testCase.ID)

        val errorRate = if (prevalence == 0) 0 else 1 - errors.toFloat / prevalence.toFloat
        errorRatesBuffer.append(errorRate)

        testCaseSerializationBuffer.append(
          Seq(
            " " + trigger.label + " { id: " + trigger.iri + " } ",
            " " + testApproachCollection(testCase.validatorID).toString + " ",
            prevalence.toString,
            errors.toString,
            errorRate.toString
          )
        )
      })
    })

    val errorRates = errorRatesBuffer.toArray

    println(s" -- Avg. Error Rate ${errorRates.sum / errorRates.length}")

    println(Tabulator.format(testCaseSerializationBuffer))
  }
}
