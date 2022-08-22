package org.dbpedia.validation.construct.report

import org.dbpedia.validation.construct.model.TestScore
import org.dbpedia.validation.construct.report.formats.{HTMLTestReport, ReportFormat}
import org.dbpedia.validation.construct.tests.suites.TestSuite

import scala.tools.nsc.interpreter.OutputStream

object ReportWriter {

  def write(label: String, testScore: TestScore, testSuite: TestSuite, reportFormat: ReportFormat.Value, outputStream: OutputStream): Unit = {

    reportFormat match {
      case ReportFormat.HTML => HTMLTestReport.build(label, testScore, testSuite, outputStream)
      case ReportFormat.GENERIC => writeGENERICReport(label, testScore, testSuite, outputStream)
      case ReportFormat.RDF => writeRDFReport(label, testScore, testSuite, outputStream)
    }
  }

  private def writeRDFReport(label: String = "", testScore: TestScore, testSuite: TestSuite, outputStream: OutputStream): Unit = {
    //TODO
  }

  private def writeGENERICReport(label: String = "", testScore: TestScore, testSuite: TestSuite, outputStream: OutputStream): Unit = {
    //TODO
  }
}
