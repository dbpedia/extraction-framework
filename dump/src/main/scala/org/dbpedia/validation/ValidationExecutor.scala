package org.dbpedia.validation

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SQLContext
import org.dbpedia.validation.TestSuiteImpl.TestSuite
import org.dbpedia.validation.TriggerImpl.TriggerType

object ValidationExecutor {

  def testIris(pathToFlatTurtleFile: String, testModelPaths: Array[String])
              (implicit sqlContext: SQLContext): Array[TestReport] = {

    val testSuite = TestSuiteFactory.loadTestSuite(testModelPaths)

    testIris(pathToFlatTurtleFile, testSuite)

  }

  def testIris(pathToFlatTurtleFile: String, testSuite: TestSuite)
              (implicit sqlContext: SQLContext): Array[TestReport] = {

    val partLabels = Array[String]("SUBJECT TEST CASES","PREDICATE TEST CASES","OBJECT TEST CASES")

    import sqlContext.implicits._


    val brdcstTestSuit: Broadcast[TestSuite] = sqlContext.sparkSession.sparkContext.broadcast(testSuite)

    val spoBasedDataset =
      sqlContext.read.textFile(pathToFlatTurtleFile)
        .repartition(Runtime.getRuntime.availableProcessors()*3)
        .map(_.trim).filter( ! _.startsWith("#") ).filter( ! _.equals("") ).map(prepareFaltTurtleLine)

    val zero = {
      TestReport(
        cnt = 0,
        coverage = 0,
        prevalence = Array.fill[Long](brdcstTestSuit.value.maxTriggerID + 1)(0),
        succeeded = Array.fill[Long](brdcstTestSuit.value.maxTestCaseID + 1)(0)
      )
    }

    val testReports: IndexedSeq[TestReport] = {

      (0 until 3).map(

        column => {
          spoBasedDataset.map(_ (column)).distinct().filter(_ != null).map(

            nTriplePart => { validateNTriplePart(nTriplePart, brdcstTestSuit, partLabels(column)) }

          ).rdd.fold(zero)( _+_ )
        }
      )
    }

    testReports.toArray
  }

  /**
    * Assumption: The whitespace following subject, predicate, and object must be a single space, (U+0020).
    * All other locations that allow whitespace must be empty. (https://www.w3.org/TR/n-triples/#canonical-ntriples)
    */
  def prepareFaltTurtleLine(line: String): Array[String] = {

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

          if ( a.endsWith(".") ) a.dropRight(1).trim else a
        }

        if ( aa.startsWith("<") ) aa.drop(1).dropRight(1) else aa
      }
    }
    catch {
      case ae: ArrayIndexOutOfBoundsException => println(line); ae.printStackTrace()
    }

    Array(s,p,o)
  }

  def validateNTriplePart(nTriplePart: String, brdTestSuite: Broadcast[TestSuite], part: String): TestReport = {

    var covered = false

    val testSuite = brdTestSuite.value

    val prevalence = Array.fill[Long](testSuite.maxTriggerID + 1)(0)
    val succeded = Array.fill[Long](testSuite.maxTestCaseID + 1)(0)


    val nTriplePartType = {

      if ( nTriplePart.startsWith("\"") ) TriggerType.LITERAL
      else TriggerType.IRI
    }


    testSuite.triggerCollection.filter(_.TYPE == nTriplePartType).foreach(

      trigger => {

        if( trigger.isTriggered(nTriplePart) ) {

          if ( trigger.iri != "__GENERIC_IRI__" ) covered = true

          prevalence(trigger.ID) = 1

          trigger.testCases.foreach(

            testCase => {

              val success = testSuite.testApproachCollection(testCase.testAproachID).run(nTriplePart)

              // TODO count overlap store succeeded before and then= add all together
              if (success) succeded(testCase.ID) = 1
            }
          )
        }
      }
    )

//    if( ! covered && nTriplePartType.equals(TriggerType.IRI) ) System.err.println(part+" "+s"UNCOVERED $nTriplePart")

    TestReport(
      cnt = 1, coverage = {if (covered) 1 else 0},
      prevalence, succeded
    )
  }
}
