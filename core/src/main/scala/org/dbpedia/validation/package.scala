package org.dbpedia

import org.dbpedia.validation.TestCaseImpl.TestApproach
import org.dbpedia.validation.TriggerImpl.Trigger

import scala.collection.mutable.ArrayBuffer

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
       |SELECT ?trigger ?label ?comment (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns) {
       |  ?trigger
       |     a            v:RDF_IRI_Trigger ;
       |     trigger:pattern    ?pattern ;
       |     rdfs:label   ?label ;
       |     rdfs:comment ?comment .
       |
       |} GROUP BY ?trigger ?label ?comment
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

  def triggeredValidatorsQueryStr(triggerIri: String): String =
    s"""$prefixDefinition
       |
       |SELECT ?validator {
       |
       |	?s v:trigger <$triggerIri> ;
       |     v:validator ?validator
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

  case class TestReport(cnt: Long, coverage: Long, prevalence: Array[Long], succeded: Array[Long] ) {

    def +(testReport: TestReport): TestReport = {

      TestReport(
        cnt + testReport.cnt,
        coverage + testReport.coverage,
        prevalence.zip(testReport.prevalence).map { case (x, y) => x + y },
        succeded.zip(testReport.succeded).map { case (x, y) => x + y }
      )
    }
  }

  def formatTestReport( label : String,
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

        testCaseSerializationBuffer.append(
          Seq(
            " "+trigger.iri+" ",
            " missing validator ",
            testReport.prevalence(trigger.ID).toString,
            testReport.prevalence(trigger.ID).toString,
            "0.0"
          )
        )
      }

      trigger.testCases.foreach( testCase => {

        val prevalence = testReport.prevalence(trigger.ID)
        val success = testReport.succeded(testCase.ID)

        val errorRate = if ( prevalence == 0 ) 0 else 1-success.toFloat/prevalence.toFloat
        errorRatesBuffer.append(errorRate)

        testCaseSerializationBuffer.append(
          Seq(
            " "+trigger.iri+" ",
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

}

