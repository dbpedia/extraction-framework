package org.dbpedia.validation.construct.tests.suites

import org.apache.jena.rdf.model.Model
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SQLContext
import org.dbpedia.validation.construct.model.{TestCase, TestScore, TriggerType}
import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.validators.Validator
import org.dbpedia.validation.construct.tests.generators.NTripleTestGenerator


class NTripleTestSuite(override val triggerCollection: Array[Trigger],
                       override val validatorCollection: Array[Validator],
                       override val testCaseCollection: Array[TestCase]) extends TestSuite {

  def test(path: String)(implicit SQLContext: SQLContext): Array[TestScore] = {

    val partLabels = Array[String]("SUBJECT TEST CASES", "PREDICATE TEST CASES", "OBJECT TEST CASES")

    import SQLContext.implicits._

    val brdTestCaseCount: Broadcast[Int] = SQLContext.sparkContext.broadcast(testCaseCollection.length)
    val brdTriggerCollection: Broadcast[Array[Trigger]] = SQLContext.sparkContext.broadcast(triggerCollection)
    val brdValidatorCollection: Broadcast[Array[Validator]] = SQLContext.sparkContext.broadcast(validatorCollection)

    val spoBasedDataset =
      SQLContext.read.textFile(path)
        .repartition(Runtime.getRuntime.availableProcessors() * 3)
        .map(_.trim).filter(!_.startsWith("#")).filter(!_.equals("")).map(prepareFlatTerseLine)

    val zero = {
      TestScore(
        total = 0,
        covered = 0,
        valid = 0,
        prevalenceOfTriggers = Array.fill[Long](brdTriggerCollection.value.length)(0),
        errorsOfTestCases = Array.fill[Long](brdTestCaseCount.value)(0)
      )
    }

    val testReports: Array[TestScore] = {
      //      val list = List(0,1,2)
      Array(0,1,2).map(column => {
        spoBasedDataset.map(_ (column))
          .distinct()
          .filter(_ != null)
          .map(nTriplePart => {
            validateNTriplePart(
              nTriplePart,
              brdTestCaseCount.value,
              brdTriggerCollection.value,
              brdValidatorCollection.value,
              partLabels(column)
            )
          }).rdd.fold(zero)(_ + _)
      })
    }

    testReports.toArray
  }

  /**
   * Assumption: The whitespace following subject, predicate, and object must be a single space, (U+0020).
   * All other locations that allow whitespace must be empty. (https://www.w3.org/TR/n-triples/#canonical-ntriples)
   */
  def prepareFlatTerseLine(line: String): Array[String] = {

    val spo = line.split(">", 3)

    var s: String = null
    var p: String = null
    var o: String = null

    try {
      s = spo(0).drop(1)
      p = spo(1).trim.drop(1)
      o = {

        val aa = {

          val a = spo(2).trim

          if (a.endsWith(".")) a.dropRight(1).trim else a
        }

        if (aa.startsWith("<")) aa.drop(1).dropRight(1) else aa
      }
    }
    catch {
      case ae: ArrayIndexOutOfBoundsException => println(line); ae.printStackTrace()
    }

    Array(s, p, o)
  }

  def validateNTriplePart(
                           nTriplePart: String,
                           testCaseCount: Int,
                           triggerCollection: Array[Trigger],
                           validatorCollection: Array[Validator],
                           constructLabel: String): TestScore = {

    var covered = false
    var valid = true

    val prevalenceOfTrigger = Array.fill[Long](triggerCollection.length)(0)
    val errorsOfTestCase = Array.fill[Long](testCaseCount)(0)

    val nTriplePartType = {
      if (nTriplePart.startsWith("\"")) TriggerType.LITERAL
      else TriggerType.IRI
    }

    triggerCollection.filter(_.TYPE == nTriplePartType).foreach(

      trigger => {

        if (trigger.isTriggered(nTriplePart)) {

          if (trigger.iri != "#GENERIC_IRI_TRIGGER") covered = true

          prevalenceOfTrigger(trigger.ID) = 1

          trigger.testCases.foreach(

            testCase => {

              val success = validatorCollection(testCase.validatorID).run(nTriplePart)

              // TODO count overlap store succeeded before and then=2 add all together
              if (!success) {
                valid = false
                errorsOfTestCase(testCase.ID) = 1
              }
            }
          )
        }
      }
    )

    TestScore(
      total = 1,
      covered = if (covered) 1 else 0,
      valid = if (valid && covered) 1 else 0,
      prevalenceOfTriggers = prevalenceOfTrigger,
      errorsOfTestCases = errorsOfTestCase)
  }
}