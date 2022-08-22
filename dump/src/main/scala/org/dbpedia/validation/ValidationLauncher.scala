//package org.dbpedia.validation
//
//import java.io.{File, FileWriter}
//
//import org.apache.spark.sql.{SQLContext, SparkSession}
//
//
//case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
//case class SPO(s: String, p: String, o: String)
//
//case class ValidationConfig(
//                             pathToFlatTurtleFile: String = null,
//                             pathToTestCaseFile: Seq[String] = null,
//                             pathToResultFolder: String = null
//                           )
//
///**
//  * Basic Usage:
//  *
//  * cd core/ && mvn scala:run -Dlauncher=iriTest -DaddArgs="-t|pathToTestFile|pathToFlatTurtleFile"
//  *     pathToTestFile         Path to rdf test case file
//  *     pathToFlatTurtleFile   Any un/compressed flat Turtle/NTriples file. Globbing possible (e.g dir/\*.ttl.bz2)
//  * Adjust memory (launcher Xmx arg) accordingly inside pom. Higher is better.
//  * Handling LogLevel by using core/src/main/resources/template.log4j.properties
//  */
//object ValidationLauncher {
//
//  def main(args: Array[String]): Unit = {
//
//    val optionParser: OptionParser[ValidationConfig] = new OptionParser[ValidationConfig]("iriTest") {
//
//      head("iriTest", "0.1")
//
//      arg[String]("<flat-turtle-files>").required().maxOccurs(1).action((s, p) => p.copy(pathToFlatTurtleFile = s))
//        .text("Any un/compressed flat Turtle/NTriples file. Globing possible (e.g dir/*.ttl.bz2)")
//
//      opt[Seq[String]]('t', "testModel").required().maxOccurs(1).action((s, p) =>  p.copy(pathToTestCaseFile = s))
//        .text("Path to rdf test case file")
//
//
////      opt[String]('f', "format").maxOccurs(1).action((s, p) =>  p.copy(pathToResultFolder = s))
////        .text("format: rdf, html")
//    }
//
//    optionParser.parse(args,ValidationConfig()) match {
//
//      case Some(config) =>
//
//        println("---------------------------")
//        println(" Spark based IRI form test ")
//        println("---------------------------")
//
//        val hadoopHomeDir = new File("./.haoop/")
//        hadoopHomeDir.mkdirs()
//        System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)
//
//        val sparkSession = SparkSession.builder()
//          .config("hadoop.home.dir", "./.hadoop")
//          .config("spark.local.dir", "./.spark")
//          .appName("Test Iris").master("local[*]").getOrCreate()
//        sparkSession.sparkContext.setLogLevel("WARN")
//
//        sqlContext.read.load()
//        val sqlContext: SQLContext = sparkSession.sqlContext
//
//        val testSuite = TestSuiteFactory.loadTestSuite(config.pathToTestCaseFile.toArray)
//
//        val testReports = ValidationExecutor.testIris(config.pathToFlatTurtleFile, testSuite)(sqlContext)
//
//        val partLabels = Array[String]("SUBJECT TEST CASES","PREDICATE TEST CASES","OBJECT TEST CASES")
//
//        Array.tabulate(testReports.length) {
//
//          //      i => formatTestReport2(partLabels(i),counts(i),testSuite.triggerCollection,testSuite.testApproachCollection)
//          i => {
//            val (html,_) = buildModReport(partLabels(i), testReports(i), testSuite.triggerCollection, testSuite.testApproachCollection)
//
//            if(config.pathToResultFolder != null) {
//              val fw = new FileWriter(s"${config.pathToResultFolder}/output_$i.html",false)
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
//}
