package org.dbpedia.validation

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SQLContext
import org.dbpedia.validation.TestSuiteImpl.TestSuite
import org.dbpedia.validation.TriggerImpl.TriggerType

object ValidationExecutor {

  def testIris(pathToFlatTurtleFile: String, testModelPaths: Array[String])
              (implicit sqlContext: SQLContext): Array[TestReport] = {

    val partLabels = Array[String]("SUBJECT TEST CASES","PREDICATE TEST CASES","OBJECT TEST CASES")

    import sqlContext.implicits._

    val testSuite = TestSuiteFactory.loadTestSuite(testModelPaths)

    val brdcstTestSuit: Broadcast[TestSuite] = sqlContext.sparkSession.sparkContext.broadcast(testSuite)

    val spoBasedDataset =
      sqlContext.read.textFile(pathToFlatTurtleFile)
        .repartition(Runtime.getRuntime.availableProcessors()*3)
        .filter(! _.startsWith("#")).filter( ! _.trim.equals("") ).map(prepareFaltTurtleLine)

    val zero = {
      TestReport(
        cnt = 0,
        coverage = 0,
        prevalence = Array.fill[Long](brdcstTestSuit.value.maxTriggerID + 1)(0),
        succeded = Array.fill[Long](brdcstTestSuit.value.maxTestCaseID + 1)(0)
      )
    }

    val counts: IndexedSeq[TestReport] = {

      (0 until 3).map(

        column => {
          spoBasedDataset.map(_ (column)).distinct().filter(_ != null).map(

            nTriplePart => { validateNTriplePart(nTriplePart, brdcstTestSuit, partLabels(column)) }

          ).rdd.fold(zero)( _+_ )
        }
      )
    }

    Array.tabulate(counts.length){

      i => formatTestReport(partLabels(i),counts(i),testSuite.triggerCollection,testSuite.testApproachCollection)
    }

    counts.toArray
  }

  /**
    * Assumption: The whitespace following subject, predicate, and object must be a single space, (U+0020).
    * All other locations that allow whitespace must be empty. (https://www.w3.org/TR/n-triples/#canonical-ntriples)
    */
  def prepareFaltTurtleLine(line: String): Array[String] = {
    val spo = line.split(" ", 3)

    var s: String = null
    var p: String = null
    var o: String = null

    try {
      if (spo(0).startsWith("<")) s = spo(0).substring(1, spo(0).length - 1)
      if (spo(1).startsWith("<")) p = spo(1).substring(1, spo(1).length - 1)
      if (spo(2).startsWith("<")) o = spo(2).substring(1, spo(2).length - 3)
      else if (spo(2).startsWith("\"") ) o = spo(2).substring(0,spo(2).length -2)
    }
    catch {
      case ae: ArrayIndexOutOfBoundsException => println(line)
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

              if (success) succeded(testCase.ID) = 1
            }
          )
        }
      }
    )

    if( ! covered && nTriplePartType.equals(TriggerType.IRI) ) System.err.println(part+" "+s"UNCOVERED $nTriplePart")

    TestReport(
      cnt = 1, coverage = {if (covered) 1 else 0},
      prevalence, succeded
    )
  }
}
