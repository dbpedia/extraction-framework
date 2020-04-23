package org.dbpedia.validation.construct.report.formats

import java.nio.charset.{StandardCharsets}
import java.text.SimpleDateFormat
import java.util.Calendar

import org.dbpedia.validation.construct.model.{TestCaseType, TestScore}
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

      if (testCase.TYPE == TestCaseType.GENERIC) {
        genericTests.append(tableRow)
      } else {
        customTests.append(tableRow)
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
         |  <li>Total Constructs: ${testScore.total}
         |</ul>
         |<h4>Generic Test Cases</h4>
         |<ul>
         |  <li>Erroneous_Constructs/Covered_Constructs: TODO
         |  <li>Total_Errors/Covered_Constructs: TODO
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

    outputStream.write(
      s"""</tbody>
         |</table>
         |<br>
         |<h4>Custom Test Cases</h4>
         |<strong>Overall</strong>
         |<ul>
         |  <li>Coverage: ${testScore.coverage} ( ${testScore.covered} covered of ${testScore.total} total )
         |  <li>Coverage (IRIs): TODO
         |  <li>Erroneous_Constructs/Covered_Constructs: TODO
         |  <li>Total_Errors/Covered_Constructs: TODO
         |</ul>
         |<strong>IRI Compliance</strong>
         |<ul>
         |  <li>Erroneous_Constructs/Covered_Constructs: TODO
         |  <li>Total_Errors/Covered_Constructs: TODO
         |</ul>
         |<strong>Vocab Usage</strong>
         |<ul>
         |  <li>Erroneous_Constructs/Covered_Constructs: TODO
         |  <li>Total_Errors/Covered_Constructs: TODO
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
