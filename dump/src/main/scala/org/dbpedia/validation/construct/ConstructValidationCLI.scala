package org.dbpedia.validation.construct

import java.io.File

import org.apache.jena.rdf.model.ModelFactory
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.dbpedia.validation.construct.tests.TestSuiteFactory
import scopt.OptionParser

object ConstructValidationCLI {

  case class ConstructValidationConfig(flatTripleFilePaths: Option[String] = None,
                                       testCaseFilePaths: Seq[String] = Seq(),
                                       reportFilePath: Option[String] = None,
                                       reportFormat: String = "html")

  def main(args: Array[String]): Unit = {

    val optionParser: OptionParser[ConstructValidationConfig] = new OptionParser[ConstructValidationConfig]("ConstructValidator") {

      head("iriTest", "0.1")

      arg[String]("<flat-turtle-files>").required().maxOccurs(1).action((s, p) => p.copy(flatTripleFilePaths = Some(s)))
        .text("Any un/compressed flat Turtle/NTriples file. Globing possible (e.g dir/*.ttl.bz2)")

      opt[Seq[String]]('t', "testModel")
        .required()
        .maxOccurs(1)
        .action((s, p) => p.copy(testCaseFilePaths = s))
        .text("Path to rdf test case file")

      opt[String]( "reportFormat")
        .maxOccurs(1)
        .action((s, p) => p.copy(reportFormat = s))
        .text("format: rdf, html")
        .validate( x => {
          if(x == "html" || x == "rdf") success
          else failure("Value <format> must be one of {html, rdf}")
        })

      opt[String]('r', "reportPath")
        .maxOccurs(1)
        .action((s, p) => p.copy(reportFilePath = Some(s)))
        .text("format: rdf, html")
    }

    optionParser.parse(args, ConstructValidationConfig()) match {

      case Some(config) =>

        println("---------------------------")
        println(" Spark-based IRI form test ")
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

        val testModel = {
          val model = ModelFactory.createDefaultModel()
          config.testCaseFilePaths.foreach(model.read)
          model
        }
    }
  }

//        val testSuite = TestSuiteFactory.create(testModel,TestSuiteFactory.TestSuiteType.NTriples)
//
//        val testReports = testSuite.test(config.flatTripleFilePaths.get)(sqlContext) //.test(config.flatTripleFilePaths.get)(sqlContext)
//
//        val partLabels = Array[String]("SUBJECT TEST CASES", "PREDICATE TEST CASES", "OBJECT TEST CASES")
//
//        Array.tabulate(testReports.length) {
//
//          //      i => formatTestReport2(partLabels(i),counts(i),testSuite.triggerCollection,testSuite.testApproachCollection)
//          i => {
//            val (html, _) = buildModReport(partLabels(i), testReports(i), testSuite.triggerCollection, testSuite.testApproachCollection)
//
//            if (config.pathToResultFolder != null) {
//              val fw = new FileWriter(s"${config.pathToResultFolder}/output_$i.html", false)
//              fw.write(html.mkString)
//              fw.close()
//            } else {
//              println()
//              println(partLabels(i))
//              println(html)
//            }
//          }
//        }
//
//      case _ => optionParser.showUsage()
//    }
//  }
}
