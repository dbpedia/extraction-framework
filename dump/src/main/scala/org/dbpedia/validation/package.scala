package org.dbpedia

import java.text.SimpleDateFormat
import java.util.Calendar

import org.apache.jena.rdf.model.{Model, ModelFactory, ResourceFactory}
import org.apache.jena.vocabulary.RDFS
import org.dbpedia.validation.TestCaseImpl.TestApproach
import org.dbpedia.validation.TriggerImpl.{Trigger, TriggerType}

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

//TODO cleanup
package object validation {

  type TriggerIRI = String
  type ValidatorIRI = String

  private val prefixVocab: String = "http://dev.vocab.org/"

  private def prefixDefinition: String =
    s"""PREFIX v: <$prefixVocab>
       |PREFIX trigger: <http://dev.vocab.org/trigger/>
       |PREFIX validator: <http://dev.vocab.org/validator/>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |PREFIX dataid-mt: <http://dataid.dbpedia.org/ns/mt#>
     """.stripMargin

  def iriTriggerQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT ?trigger ?label ?comment
       |  (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns)
       |{
       |  ?trigger
       |     a            v:RDF_IRI_Trigger ;
       |     trigger:pattern    ?pattern ;
       |     Optional{ ?trigger rdfs:label ?label . }
       |     Optional{ ?trigger rdfs:comment ?comment . }
       |
       |
       |} GROUP BY ?trigger ?label ?comment
     """.stripMargin

  def literalTriggerQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT DISTINCT ?trigger ?datatype ?label ?comment
       |{
       |  ?trigger
       |     a            v:RDF_Literal_Trigger ;
       |     trigger:datatype    ?datatype ;
       |     Optional{ ?trigger rdfs:label ?label . }
       |     Optional{ ?trigger rdfs:comment ?comment . }
       |
       |}
     """.stripMargin

  def iriValidatorQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT Distinct ?validator ?comment
       |  (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns)
       |  (GROUP_CONCAT(DISTINCT ?oneOfVocab; SEPARATOR="\t") AS ?oneOfVocabs)
       |  (GROUP_CONCAT(DISTINCT ?doesNotContain; SEPARATOR="\t") AS ?doesNotContains)
       |{
       |  ?validator
       |     a v:IRI_Validator .
       |     Optional{ ?validator rdfs:comment ?comment }
       |     Optional{ ?validator v:doesNotContain ?doesNotContain . }
       |     Optional{ ?validator v:pattern ?pattern . }
       |     Optional{ ?validator v:oneOfVocab ?oneOfVocab . }
       |} GROUP BY ?validator ?comment
     """.stripMargin

  def literalValidatorQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT Distinct ?validator ?comment ?pattern
       |{
       |  ?validator
       |     a v:Datatype_Literal_Validator .
       |     Optional{ ?validator rdfs:comment ?comment }
       |     Optional{ ?validator v:pattern ?pattern }
       |}
     """.stripMargin

  def triggeredValidatorsQueryStr(triggerIri: String, isBlank: Boolean): String = {

    s"""$prefixDefinition
       |
       |SELECT ?validator {
       |
       |  ?s a v:TestGenerator ;
       |     v:trigger ${if (isBlank) s"_:$triggerIri" else s"<$triggerIri>" } ;
       |     v:validator ?validator .
       |}
     """.stripMargin
  }

  def testGeneratorQueryStr: String =
    s"""$prefixDefinition
       |
       |SELECT ?generator ?trigger ?validator
       |{
       |  ?generator v:trigger ?trigger ;
       |             v:validator ?validator .
       |
       |}
     """.stripMargin

  def oneOfVocabQueryStr: String =
    """PREFIX owl: <http://www.w3.org/2002/07/owl#>
      |
      |SELECT DISTINCT ?property {
      |
      |  ?property a  <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .
      |  #FILTER ( ?type IN ( owl:DatatypeProperty, owl:ObjectProperty ) )
      |}"""
     .stripMargin

  // TODO prevalence and succeeded access with TestCaseID and TriggerID types (wrapper)
  case class TestReport(cnt: Long, coverage: Long, prevalence: Array[Long], succeeded: Array[Long] ) {

    def +(testReport: TestReport): TestReport = {

      TestReport(
        cnt + testReport.cnt,
        coverage + testReport.coverage,
        prevalence.zip(testReport.prevalence).map { case (x, y) => x + y },
        succeeded.zip(testReport.succeeded).map { case (x, y) => x + y }
      )
    }
  }

  def buildTableReport( label : String,
                        testReport: TestReport,
                        triggerCollection: Array[Trigger],
                        testApproachCollection: Array[TestApproach] ) : Unit = {

    println()
    print(s"$label")
    print(s" -- Coverage: ${testReport.coverage.toFloat/testReport.cnt.toFloat} " +
      s"(${testReport.coverage} triggered of ${testReport.cnt} total)")

    val errorRatesBuffer = ArrayBuffer[Float]()
    val testCaseSerializationBuffer = ArrayBuffer[Seq[String]]()

    testCaseSerializationBuffer.append(
      Seq("Trigger","Test Approach","Prevalence", "Errors", "Error Rate")
    )

    triggerCollection.foreach( trigger => {

      if ( trigger.testCases.length == 0 ) {

        // Does not increase the error rate
        testCaseSerializationBuffer.append(
          Seq(
            " "+trigger.label+" { id: "+trigger.iri+" } ",
            " missing validator ",
            testReport.prevalence(trigger.ID).toString,
            testReport.prevalence(trigger.ID).toString,
            "0.0"
          )
        )
      }

      trigger.testCases.foreach( testCase => {

        val prevalence = testReport.prevalence(trigger.ID)
        val success = testReport.succeeded(testCase.ID)

        val errorRate = if ( prevalence == 0 ) 0 else 1-success.toFloat/prevalence.toFloat
        errorRatesBuffer.append(errorRate)

        testCaseSerializationBuffer.append(
          Seq(
            " "+trigger.label+" { id: "+trigger.iri+" } ",
            " "+testApproachCollection(testCase.testAproachID).toString+" ",
            prevalence.toString,
            (prevalence-success).toString,
            errorRate.toString
          )
        )
      })
    })

    val errorRates = errorRatesBuffer.toArray

    println(s" -- Avg. Error Rate ${errorRates.sum/errorRates.length}")

    println(Tabulator.format(testCaseSerializationBuffer))
  }

  def buildModReport( label: String,
                      testReport: TestReport,
                      triggerCollection: Array[Trigger],
                      testApproachCollection: Array[TestApproach]
                    ) : (StringBuilder,Float) = {

    val errorBuffer = ArrayBuffer[Float]()
    val testCaseSerializationBuffer = ArrayBuffer[TableRow]()
    val genericTestCaseSerializationBuffer = ArrayBuffer[TableRow]()

    triggerCollection.foreach( trigger => {

      if ( trigger.testCases.length == 0 ) {

        // Does not increase the error rate
        if( trigger.iri == "__GENERIC_IRI__" || trigger.iri == "__GENERIC_LITERAL__") {
          genericTestCaseSerializationBuffer.append(
            TableRow(
              0.0f,
              testReport.prevalence(trigger.ID),
              0,
              "missing validator",
              trigger.label + " { id: " + trigger.iri + " }"
            )
          )
        } else {
          testCaseSerializationBuffer.append(
            TableRow(
              0.0f,
              testReport.prevalence(trigger.ID),
              0,
              "missing validator",
              trigger.label + " { id: " + trigger.iri + " }"
            )
          )
        }
      }

      trigger.testCases.foreach( testCase => {

        //        println("methodType",testApproachCollection(testCase.testAproachID).METHOD_TYPE)
        val prevalence = testReport.prevalence(trigger.ID)
        val success = testReport.succeeded(testCase.ID)

        val errorRate = if ( prevalence == 0 ) 0 else 1-success.toFloat/prevalence.toFloat

        if( trigger.iri == "__GENERIC_IRI__" || trigger.iri == "__GENERIC_LITERAL__") {
          genericTestCaseSerializationBuffer.append(
            TableRow(
              errorRate,
              prevalence,
              prevalence - success,
              testApproachCollection(testCase.testAproachID).toString,
              trigger.label + " { id: " + trigger.iri + " }"
            )
          )
        } else {
          errorBuffer.append(prevalence-success)

          testCaseSerializationBuffer.append(
            TableRow(
              errorRate,
              prevalence,
              prevalence - success,
              testApproachCollection(testCase.testAproachID).toString,
              trigger.label + " { id: " + trigger.iri + " }"
            )
          )
        }
      })
    })

//    testCaseSerializationBuffer.append(
//      Seq("Trigger","Test Approach","Prevalence", "Errors", "Error Rate")
//    )

    val errorRate =  errorBuffer.toArray.sum / testReport.cnt.toFloat

    /**
     * excluded GENERIC_IRI but inculded GENERIC_LITERAL
     * into coverage
     */
    val coverage = testReport.coverage.toFloat / testReport.cnt.toFloat
    val stringBuilder = new StringBuilder

    stringBuilder.append(
      s"""|<!DOCTYPE html>
          |<html>
          |<head>
          |<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
          |<link rel="stylesheet" href="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.css">
          |</head>
          |<body>
          |<h3>$label</h3>
          |<ul>
          |  <li>Timestamp: ${new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(Calendar.getInstance().getTime )}
          |  <li>Coverage: $coverage ( ${testReport.coverage} triggered of ${testReport.cnt} total )
          |  <li>Avg. Error Rate: $errorRate
          |</ul>
          |""".stripMargin)

    stringBuilder.append(
      """<table
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
        |""".stripMargin)

    //    testCaseSerializationBuffer.toArray
    genericTestCaseSerializationBuffer.toArray
      .sortWith(_.prevalence > _.prevalence)
      .sortWith(_.errors > _.errors)
      .sortWith(_.errorRate > _.errorRate)
      .foreach(row => stringBuilder.append(row.toString) )

    stringBuilder.append(
      """</tbody>
        |</table>
        |<hr>
        |""".stripMargin)

    stringBuilder.append(
      """<table
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
        |""".stripMargin)

//    testCaseSerializationBuffer.toArray
    testCaseSerializationBuffer.toArray
      .sortWith(_.prevalence > _.prevalence)
      .sortWith(_.errors > _.errors)
      .sortWith(_.errorRate > _.errorRate)
      .foreach(row => stringBuilder.append(row.toString) )

    stringBuilder.append(
      """</tbody>
        |</table>
        |<script src="https://code.jquery.com/jquery-3.3.1.min.js"></script>
        |<script src="https://unpkg.com/bootstrap-table@1.16.0/dist/bootstrap-table.min.js"></script>
        |<body>
        |""".stripMargin)

    (stringBuilder,errorRate)
  }

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

  type IRI = String

  class AdvancedJenaModel(m: Model) {

    def simpleStatement(s: IRI, p: IRI, o: IRI): Unit = {
      m.add(
        ResourceFactory.createStatement(
          ResourceFactory.createResource(s),
          ResourceFactory.createProperty(p),
          ResourceFactory.createResource(o)
        )
      )
    }

    def simpleLiteralStatement(s: IRI, p: IRI, o: Any): Unit = {

      m.add(
        ResourceFactory.createStatement(
          ResourceFactory.createResource(s),
          ResourceFactory.createProperty(p),
          ResourceFactory.createTypedLiteral(o)
        )
      )
    }
  }

  def buildRDFReport( label : String,
                      testReport: TestReport,
                      triggerCollection: Array[Trigger],
                      testApproachCollection: Array[TestApproach] ) : Model = {


    implicit def toAdvancedJenaModel(m: Model): AdvancedJenaModel = new AdvancedJenaModel(m)

    val model = ModelFactory.createDefaultModel()

    triggerCollection.foreach(

      trigger => {

        val triggerPrevalence = testReport.prevalence(trigger.ID)

        model.simpleLiteralStatement(trigger.iri, RDFS.label.getURI, trigger.label)
        model.simpleLiteralStatement(trigger.iri, RDFS.comment.getURI, trigger.comment)
        model.simpleLiteralStatement(trigger.iri, ReportVocab.trigger+"prevalence", triggerPrevalence)

        trigger.testCases.foreach(

          testCase => {

            val testCaseIRI = ReportVocab.TestCase+testCase.ID
            model.simpleStatement(trigger.iri, ReportVocab.trigger+"hasTestCase", testCaseIRI)

            model.simpleLiteralStatement(testCaseIRI,
              ReportVocab.testCase+"id", testCase.ID)
            model.simpleStatement(testCaseIRI,
              ReportVocab.testCase+"hasTrigger", trigger.iri)
            model.simpleStatement(testCaseIRI,
              ReportVocab.testCase+"hasApproach", ReportVocab.TestApproach+testCase.testAproachID )
            model.simpleLiteralStatement(testCaseIRI,
              ReportVocab.testCase+"errors", triggerPrevalence-testReport.succeeded(testCase.ID) )
          }
        )
      }
    )

    testApproachCollection.foreach(

      testApproach => {

        val testApproachIRI = ReportVocab.TestApproach+testApproach.ID
        model.simpleLiteralStatement(testApproachIRI,
          ReportVocab.testApproach+"id", testApproach.ID)
        model.simpleLiteralStatement(testApproachIRI,
          ReportVocab.testApproach+"info", testApproach.info() )
      }
    )

    model
  }

  object ReportVocab {

    val base: String = "http://eval.dbpedia.org/"

    val Trigger: String =  base+"Trigger#"
    val trigger: String =  base+"trigger/"
    val TestCase: String =  base+"TestCase#"
    val testCase: String =  base+"testCase/"
    val TestApproach: String =  base+"testapproach#"
    val testApproach: String =  base+"testapproach/"
  }
}
