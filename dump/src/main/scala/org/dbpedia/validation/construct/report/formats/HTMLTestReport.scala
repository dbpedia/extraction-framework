package org.dbpedia.validation.construct.report.formats

import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar

import org.dbpedia.validation.construct.model.triggers.generic.{GenericIRITrigger, GenericLiteralTrigger}
import org.dbpedia.validation.construct.model.{TestCaseType, TestScore, TriggerType}
import org.dbpedia.validation.construct.tests.suites.TestSuite

import scala.collection.mutable.ArrayBuffer
import scala.tools.nsc.interpreter.OutputStream

object HTMLTestReport {

  type IRI = String

  case class TableRow(errorRate: String, prevalence: Long, errors: Long, approach: String, trigger: String) {

    override def toString: IRI =
      s"""<tr>
         | <td>$errorRate</td>
         | <td>$prevalence</td>
         | <td>$errors</td>
         | <td>$approach</td>
         | <td>$trigger</td>
         |</tr>
         |""".stripMargin

  }

  def pct(float: Float): String = {
    new java.math.BigDecimal(float * 100)
      .round(new java.math.MathContext(4, RoundingMode.UP))
//      .setScale(4, RoundingMode.UP)
      .toPlainString + "%"
//    match {
//      case subOne if subOne.startsWith("0.") => subOne.substring(1)
//      case greOne => greOne
//    }
  }

  def build(label: String,
            testScore: TestScore,
            testSuite: TestSuite,
            outputStream: OutputStream): Unit = {

    val customTests = ArrayBuffer[TableRow]()
    val genericTests = ArrayBuffer[TableRow]()

    var generic_total_errors: Long = 0

    var iriCount: Long = 0
    var litCount: Long = 0

    var custom_total_errors: Long = 0 // =


    testSuite.testCaseCollection.foreach(testCase => {

      val tableRow: TableRow = {

        val prevalence = testScore.prevalenceOfTriggers(testCase.triggerID)
        val errors = testScore.errorsOfTestCases(testCase.ID)
        val errorRate = if (prevalence == 0) 0f else errors.toFloat / prevalence.toFloat
        val validatorNote = testSuite.validatorCollection(testCase.validatorID).toString
        val triggerNote = testSuite.triggerCollection(testCase.triggerID).label +
          " { id: " + testSuite.triggerCollection(testCase.triggerID).iri + " }"

        TableRow(
          pct(errorRate),
          prevalence,
          errors,
          validatorNote,
          triggerNote
        )
      }

      val trigger = testSuite.triggerCollection(testCase.triggerID)

      if (testCase.TYPE == TestCaseType.GENERIC) {
        genericTests.append(tableRow)
        generic_total_errors += testScore.errorsOfTestCases(testCase.ID)

        trigger match {
          case iriT: GenericIRITrigger => iriCount = testScore.prevalenceOfTriggers(testCase.triggerID)
          case litT: GenericLiteralTrigger => litCount = testScore.prevalenceOfTriggers(testCase.triggerID)
          case _ =>
        }
      } else {
        customTests.append(tableRow)
        custom_total_errors += testScore.errorsOfTestCases(testCase.ID)
      }
    })

    outputStream.write(
      s"""<!DOCTYPE html>
         |<html>
         |<head>
         |<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
         |<link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.css">
         |</head>
         |<body>
         |<h3>$label</h3>
         |<table border="0">
         |  <tr>
         |    <td class="font-weight-bold text-right">Timestamp:</td>
         |    <td class="pl-4">${new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(Calendar.getInstance().getTime)}</td>
         |  </tr>
         |</table>
         |<h4>Generic Test Cases</h4>
         |<table border="0">
         |  <tr>
         |    <td class="font-weight-bold text-right">Total constructs:</td>
         |    <td class="pl-4">${testScore.total}</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Covered constructs:</td>
         |  <td class="pl-4">${testScore.coveredGeneric}</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Coverage:</td>
         |  <td class="pl-4">${pct(testScore.coveredGeneric / testScore.total.toFloat)} ( covered / total )</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Constructs with >=1 errors:</td>
         |  <td class="pl-4">${testScore.total - testScore.validGeneric}</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Error rate I:</td>
         |  <td class="pl-4">${pct((testScore.total - testScore.validGeneric) / testScore.total.toFloat)} ( erroneous constructs / total )</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Total errors:</td>
         |  <td class="pl-4">$generic_total_errors</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Error rate II:</td>
         |  <td class="pl-4">${pct(generic_total_errors / testScore.total.toFloat)} ( total errors / total constructs )</td>
         |  </tr>
         |</table>
         |<table
         | data-toggle="table"
         | data-search="true">
         |<thead>
         |<tr>
         | <th data-sortable="true" data-field="errorrate">Error Rate</th>
         | <th data-sortable="true" data-field="prevalence">Prevalence</th>
         | <th data-sortable="true" data-field="errors">Errors</th>
         | <th data-sortable="true" data-field="approach">Test Approach</th>
         | <th data-sortable="true" data-field="trigger">Triggered From</th>
         |</tr>
         |</thead>
         |<tbody>
         |""".stripMargin.getBytes(StandardCharsets.UTF_8))

    genericTests.toArray
      .sortWith(_.prevalence > _.prevalence)
      .sortWith(_.errors > _.errors)
      .sortWith(_.errorRate > _.errorRate)
      .foreach(row => outputStream.write(row.toString.getBytes(StandardCharsets.UTF_8)))

    //   <li>Coverage: ${testScore.coverage} ( ${testScore.covered} covered of ${testScore.total} total )
    outputStream.write(
      s"""</tbody>
         |</table>
         |<br>
         |<h4>Custom Test Cases</h4>
         |<table border="0">
         |  <tr>
         |    <td class="font-weight-bold text-right">Total IRI count:</td>
         |    <td class="pl-4">$iriCount</td>
         |  </tr>
         |  <tr>
         |    <td class="font-weight-bold text-right">Covered IRIs:</td>
         |    <td class="pl-4">${testScore.coveredCustom}</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Coverage:</td>
         |  <td class="pl-4">${pct(testScore.coveredCustom / iriCount.toFloat)} ( covered / total )</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Constructs with >=1 errors:</td>
         |  <td class="pl-4">${testScore.coveredCustom - testScore.validCustom}</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Error rate I:</td>
         |  <td class="pl-4">${pct((testScore.coveredCustom - testScore.validCustom) / testScore.coveredCustom.toFloat)} ( erroneous constructs / total )</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Total errors:</td>
         |  <td class="pl-4">$custom_total_errors</td>
         |  </tr>
         |  <tr>
         |  <td class="font-weight-bold text-right">Error rate II:</td>
         |  <td class="pl-4">${pct(custom_total_errors / testScore.coveredCustom.toFloat)} ( total errors / total constructs )</td>
         |  </tr>
         |</table>
         |<table
         | data-toggle="table"
         | data-search="true">
         |<thead>
         |<tr>
         | <th data-sortable="true" data-field="errorrate">Error Rate</th>
         | <th data-sortable="true" data-field="prevalence">Prevalence</th>
         | <th data-sortable="true" data-field="errors">Errors</th>
         | <th data-sortable="true" data-field="approach">Test Approach</th>
         | <th data-sortable="true" data-field="trigger">Triggered From</th>
         |</tr>
         |</thead>
         |<tbody>
         |""".stripMargin.getBytes(StandardCharsets.UTF_8))

    customTests.toArray
      .sortWith(_.prevalence > _.prevalence)
      .sortWith(_.errors > _.errors)
      .sortWith(_.errorRate > _.errorRate)
      .foreach(row => outputStream.write(row.toString.getBytes(StandardCharsets.UTF_8)))

    outputStream.write(
      """</tbody>
        |</table>
        |<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
        |<script src="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.js"></script>
        |<style> .float-right { float: left !important; } </style>
        |<body>
        |""".stripMargin.getBytes(StandardCharsets.UTF_8))
  }
}
