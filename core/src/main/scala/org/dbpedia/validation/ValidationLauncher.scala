package org.dbpedia.validation

import java.io.File

import org.apache.commons.cli.CommandLineParser
import org.apache.spark.sql.{SQLContext, SparkSession}
import scopt.OptionParser

case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
case class SPO(s: String, p: String, o: String)

case class ValidationConfig(pathToFlatTurtleFile: String= null,pathToTestCaseFile: String= null)

object ValidationLauncher {

  def main(args: Array[String]): Unit = {

    val optionParser: OptionParser[ValidationConfig] = new OptionParser[ValidationConfig]("iriTest") {

      head("iriTest", "0.1")

      arg[String]("<flat-turtle-files>").required().maxOccurs(1).action((s, p) => p.copy(pathToFlatTurtleFile = s))
        .text("Line based rdf FILE ( spark syntax)")

      opt[String]('t', "testCase").required().maxOccurs(1).action((s, p) =>  p.copy(pathToTestCaseFile = s))
        .text("Iri test case file")

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

        ValidationExecutor.testIris(config.pathToFlatTurtleFile, config.pathToTestCaseFile)(sqlContext)

      case _ => optionParser.showUsage()
    }
  }
}
