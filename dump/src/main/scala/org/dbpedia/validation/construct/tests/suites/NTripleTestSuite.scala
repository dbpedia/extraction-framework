package org.dbpedia.validation.construct.tests.suites

import org.apache.jena.rdf.model.Model
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SQLContext
import org.dbpedia.validation.construct.model.{Construct, TestCase, TestCaseType, TestScore, TriggerType}
import org.dbpedia.validation.construct.model.triggers.{IRITrigger, Trigger}
import org.dbpedia.validation.construct.model.validators.Validator
import org.dbpedia.validation.construct.tests.generators.NTripleTestGenerator


class NTripleTestSuite(override val triggerCollection: Array[Trigger],
                       override val validatorCollection: Array[Validator],
                       override val testCaseCollection: Array[TestCase]) extends TestSuite {

  def test(path: String)(implicit SQLContext: SQLContext): Array[TestScore] = {

    //    val partLabels = Array[String]("SUBJECT TEST CASES", "PREDICATE TEST CASES", "OBJECT TEST CASES")

    import SQLContext.implicits._

    val brdTestCaseCount: Broadcast[Int] = SQLContext.sparkContext.broadcast(testCaseCollection.length)
    val brdTriggerCollection: Broadcast[Array[Trigger]] = SQLContext.sparkContext.broadcast(triggerCollection)
    val brdValidatorCollection: Broadcast[Array[Validator]] = SQLContext.sparkContext.broadcast(validatorCollection)

    val spoBasedDataset =
      SQLContext.read.textFile(path)
        .repartition(Runtime.getRuntime.availableProcessors() * 3)
        .map(_.trim).filter(!_.startsWith("#")).filter(!_.equals("")).flatMap(prepareFlatTerseLine)

    val zero = {
      TestScore(
        total = 0,
        coveredGeneric = 0,
        coveredCustom = 0,
        validGeneric = 0,
        validCustom = 0,
        prevalenceOfTriggers = Array.fill[Long](brdTriggerCollection.value.length)(0),
        errorsOfTestCases = Array.fill[Long](brdTestCaseCount.value)(0)
      )
    }

    val testReports: Array[TestScore] = {

      //      Array(0,1,2).map(column => {
      Array(
        spoBasedDataset
          //          .map(_ (column))
          //          .distinct()
          .filter(_ != null)
          .map(nTriplePart => {
            validateNTriplePart(
              nTriplePart,
              brdTestCaseCount.value,
              brdTriggerCollection.value,
              brdValidatorCollection.value,
              ""
            )
          }).rdd.fold(zero)(_ + _)
      )
      //      })
    }

    testReports
  }



  /**
   * Assumption: The whitespace following subject, predicate, and object must be a single space, (U+0020).
   * All other locations that allow whitespace must be empty. (https://www.w3.org/TR/n-triples/#canonical-ntriples)
   */
  def prepareFlatTerseLine(line: String): Array[Construct] = {

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

    Array(Construct(s), Construct(p, Some(s), Some(o)), Construct(o))
    //Array(s, p, o)
  }

  def validateNTriplePart(
                           nTriplePart: Construct,
                           testCaseCount: Int,
                           triggerCollection: Array[Trigger],
                           validatorCollection: Array[Validator],
                           constructLabel: String): TestScore = {

    var coveredGeneric = false
    var coveredCustom = false
    var validGeneric = true
    var validCustom = true

    val prevalenceOfTrigger = Array.fill[Long](triggerCollection.length)(0)
    val errorsOfTestCase = Array.fill[Long](testCaseCount)(0)

    val nTriplePartType = {
      if (nTriplePart.self.startsWith("\"")) TriggerType.LITERAL
      else TriggerType.IRI
    }

    triggerCollection.filter(_.TYPE == nTriplePartType).foreach(

      trigger => {

        if (trigger.isTriggered(nTriplePart.self)) {

//          if (trigger.iri != "#GENERIC_IRI_TRIGGER") covered = true

          // TODO: hard coded coverage only for custom iris
          if( trigger.isInstanceOf[IRITrigger] ) coveredCustom = true
          else coveredGeneric = true

          prevalenceOfTrigger(trigger.ID) = 1

          trigger.testCases.foreach(

            testCase => {

              val success = validatorCollection(testCase.validatorID).run(nTriplePart)

              // TODO count overlap store succeeded before and then=2 add all together

              if (!success) {
                testCase.TYPE match {
                  case TestCaseType.GENERIC => validGeneric = false
                  case TestCaseType.CUSTOM => validCustom = false
                }
                errorsOfTestCase(testCase.ID) = 1
              }
            }
          )
        }
      }
    )

    // TODO log uncovered properly
//    if( ! covered && nTriplePartType == TriggerType.IRI) {
//        println(nTriplePart)
//    }

    TestScore(
      total = 1,
      coveredGeneric = if (coveredGeneric) 1 else 0,
      coveredCustom = if (coveredCustom) 1 else 0,
      validGeneric = if (validGeneric) 1 else 0,
      validCustom = if (validCustom && coveredCustom) 1 else 0,
      prevalenceOfTriggers = prevalenceOfTrigger,
      errorsOfTestCases = errorsOfTestCase)
  }
}