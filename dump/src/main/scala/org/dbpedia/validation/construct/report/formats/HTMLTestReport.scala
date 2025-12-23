package org.dbpedia.validation.construct.report.formats

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

  case class TableRow(errorRate: Float, prevalence: Long, errors: Long, approach: String, trigger: String) {

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
          errorRate,
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
         |<ul>
         |  <li>Timestamp: ${new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(Calendar.getInstance().getTime)}
         |</ul>
         |<h4>Generic Test Cases</h4>
         |<ul>
         |  <li>Total constructs: ${testScore.total}</li>
         |  <li>Covered constructs: ${testScore.coveredGeneric}</li>
         |  <li>Coverage: ${testScore.coveredGeneric / testScore.total.toFloat} covered / total</li>
         |  <li>Constructs with >=1 errors: ${(testScore.total - testScore.validGeneric)} </li>
         |  <li>Error rate I: ${(testScore.total - testScore.validGeneric) / testScore.total.toFloat} erroneous constructs / total</li>
         |  <li>Total errors: $generic_total_errors </li>
         |  <li>Error rate II:  ${generic_total_errors / testScore.total.toFloat}  total errors / total constructs </li>
         |</ul>
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
         |<ul>
         |  <li>Total IRI count: $iriCount</li>
         |  <li>Covered IRIs: ${testScore.coveredCustom} </li>
         |  <li>Coverage: ${testScore.coveredCustom / iriCount.toFloat}  covered / total </li>
         |  <li>Constructs with >=1 errors: ${(testScore.coveredCustom - testScore.validCustom)} </li>
         |  <li>Error rate I: ${(testScore.coveredCustom - testScore.validCustom) / testScore.coveredCustom.toFloat} erroneous constructs / total </li>
         |  <li>Total errors: $custom_total_errors </li>
         |  <li>Error rate II:  ${custom_total_errors / testScore.coveredCustom.toFloat}  total errors / total constructs </li>
         |</ul>
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
