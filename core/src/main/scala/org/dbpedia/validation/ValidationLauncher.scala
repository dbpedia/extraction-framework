package org.dbpedia.validation

import java.io.File

import org.apache.spark.sql.SparkSession

case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
case class SPO(s: String, p: String, o: String)

object ValidationLauncher {

  def main(args: Array[String]): Unit = {


  }

  def testIris(pathToFlatTurtleFile: String, pathToTestCases: String): Unit = {

    val hadoopHomeDir = new File("./haoop/")
    hadoopHomeDir.mkdirs()
    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)

    val sparkSession = SparkSession.builder().config("hadoop.home.dir","./hadoop")
      .appName("Test Iris").master("local[*]").getOrCreate()

    val sqlContext = sparkSession.sqlContext

    import sqlContext.implicits._

    var s: String = null

    val spoBasedDataset =
      sqlContext.read.textFile(pathToFlatTurtleFile)
        .filter(! _.startsWith("#")).map(prepareFaltTurtleLine)

    val counts: IndexedSeq[ReduceScore] = (0 until 2).map( i =>
      spoBasedDataset.map(_(i)).distinct().filter(_ != null).map(testIri)
        .reduce( (a,b) => ReduceScore(a.cntAll+b.cntAll,a.cntTrigger+b.cntTrigger,a.cntValid+b.cntValid))
    )

    val coverageTripleParts = counts.map( score => score.cntTrigger / score.cntAll)

    val coverageOverall = coverageTripleParts.sum / 3

    /*
    Iris in s p o could be overlapping
     */
    println(
      s"""
         |C_s: ${coverageTripleParts(0)} all: ${counts(0).cntAll} trg: ${counts(0).cntTrigger} vld: ${counts(0).cntValid}
         |C_p: ${coverageTripleParts(1)} all: ${counts(1).cntAll} trg: ${counts(1).cntTrigger} vld: ${counts(1).cntValid}
         |C_o: ${coverageTripleParts(2)} all: ${counts(2).cntAll} trg: ${counts(2).cntTrigger} vld: ${counts(2).cntValid}
         |C_total: $coverageOverall \t\t\t vld=doesNotContainChars
       """.stripMargin)
  }

  /**
    * Assumption: The whitespace following subject, predicate, and object must be a single space, (U+0020).
    * All other locations that allow whitespace must be empty. (https://www.w3.org/TR/n-triples/#canonical-ntriples)
    */
  def prepareFaltTurtleLine(line: String): Array[String] = {
    val spo = line.split(" ", 3)

    var s: String = null
    if (spo(0).startsWith("<")) s = spo(0).substring(1, spo(0).length - 1)

    var p: String = null
    if (spo(1).startsWith("<")) p = spo(1).substring(1, spo(1).length - 1)

    var o: String = null
    if (spo(2).startsWith("<")) o = spo(2).substring(1, spo(2).length - 3)

    Array(s,p,o)
  }

  def testIri(iriStr: String): ReduceScore = {

    ReduceScore(1,1,0)
  }
}
