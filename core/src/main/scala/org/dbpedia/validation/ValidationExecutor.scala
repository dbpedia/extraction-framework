package org.dbpedia.validation

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SQLContext

object ValidationExecutor {

  def testIris(pathToFlatTurtleFile: String, pathToTestCases: String)
              (implicit sqlContext: SQLContext): CoverageResult = {

    import sqlContext.implicits._

    val brdcstTestSuit: Broadcast[TestSuite] = sqlContext.sparkSession.sparkContext.broadcast(
      TestSuiteFactory.loadTestSuite(pathToTestCases)
    )

    val spoBasedDataset =
      sqlContext.read.textFile(pathToFlatTurtleFile)
        .repartition(Runtime.getRuntime.availableProcessors()*3)
        .filter(! _.startsWith("#")).map(prepareFaltTurtleLine)

    val counts: IndexedSeq[ReduceScore] = (0 until 3).map( i =>
      spoBasedDataset.map(_(i)).distinct().filter(_ != null).map(resource => testIri(resource, brdcstTestSuit)).rdd.
        fold(ReduceScore(0,0,0))( (a,b) => ReduceScore(a.cntAll+b.cntAll,a.cntTrigger+b.cntTrigger,a.cntValid+b.cntValid))
    )

    /*
    Iris in s p o could be overlapping
     */
    CoverageResult(
      EvalCounter(counts(0).cntAll, counts(0).cntTrigger, counts(0).cntValid),
      EvalCounter(counts(1).cntAll, counts(1).cntTrigger, counts(1).cntValid),
      EvalCounter(counts(2).cntAll, counts(2).cntTrigger, counts(2).cntValid)
    )
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
    }
    catch {
      case ae: ArrayIndexOutOfBoundsException => println(line)
    }

    Array(s,p,o)
  }

  def testIri(iriStr: String, brdTestSuite: Broadcast[TestSuite]): ReduceScore = {

    val testSuite =  brdTestSuite.value
    var triggered, valid = false

    testSuite.triggers.foreach( trigger => {

      trigger.patterns.foreach( triggerPatternStr => {

        val triggerPattern = s"$triggerPatternStr.*".r.pattern

        if ( triggerPattern.matcher(iriStr).matches ) {

          triggered = true

          trigger.validatorReferences.foreach( validatorReference => {

            // TODO: fix getOrElse workaround ! Implement TestCase
            val validatorIndex = testSuite.validatorReferencesToIndexMap.getOrElse(validatorReference,0)
            val validator: IriValidator = testSuite.validators(validatorIndex)

            validator.patterns.foreach( validatorPatternStr => {

              val validatorPattern = validatorPatternStr.r.pattern

              if (  validatorPattern.matcher(iriStr).matches ) valid = true
              // else valid = false
            })

            if( validator.oneOf.contains(iriStr) ) valid = true
          })
        }
      })
    })

    ReduceScore(1,if(triggered) 1 else 0,if(valid) 1 else 0)
  }

}
