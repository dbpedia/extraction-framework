package org.dbpedia.validation

import java.io.File

import org.apache.spark.sql.{SQLContext, SparkSession}
import scopt.OptionParser

case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
case class SPO(s: String, p: String, o: String)

case class ValidationConfig(pathToFlatTurtleFile: String = null,pathToTestCaseFile: Seq[String] = null)

/**
  * Basic Usage: 
  *   cd core/ && mvn scala:run -Dlauncher=iriTest -DaddArgs="-t|pathToTestFile|pathToFlatTurtleFile"
  *     pathToTestFile         Path to rdf test case file
  *     pathToFlatTurtleFile   Any un/compressed flatTurtle/NT-Triples file. Wildcard possible (e.g dir/\*.ttl.bz2)
  * Adjust memory (launcher Xmx arg) accordingly inside pom. Higher is better.
  * Handling LogLevel by using core/src/main/resources/template.log4j.properties
  */
object ValidationLauncher {

  def main(args: Array[String]): Unit = {

    val optionParser: OptionParser[ValidationConfig] = new OptionParser[ValidationConfig]("iriTest") {

      head("iriTest", "0.1")

      arg[String]("<flat-turtle-files>").required().maxOccurs(1).action((s, p) => p.copy(pathToFlatTurtleFile = s))
        .text("Any un/compressed flatTurtle/NT-Triples file. Wild card possible (e.g dir/*.ttl.bz2)")

      opt[Seq[String]]('t', "testModel").required().maxOccurs(1).action((s, p) =>  p.copy(pathToTestCaseFile = s))
        .text("Path to rdf test case file")

    }

    optionParser.parse(args,ValidationConfig()) match {

      case Some(config) =>

        println("---------------------------")
        println(" Spark based IRI form test ")
        println("---------------------------")

        val hadoopHomeDir = new File("./.haoop/")
        hadoopHomeDir.mkdirs()
        System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)

        val sparkSession = SparkSession.builder()
          .config("hadoop.home.dir", "./.hadoop")
          .config("spark.local.dir", "./.spark")
          .appName("Test Iris").master("local[*]").getOrCreate()
        sparkSession.sparkContext.setLogLevel("WARN")

        val sqlContext: SQLContext = sparkSession.sqlContext

        val testReports = ValidationExecutor.testIris(config.pathToFlatTurtleFile, config.pathToTestCaseFile.toArray)(sqlContext)

      case _ => optionParser.showUsage()
    }
  }
}
