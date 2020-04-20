package org.dbpedia.validation

import java.io.{ByteArrayInputStream, File, FileInputStream}

import org.apache.jena.rdf.model.{Model, ModelFactory, ResourceFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.scalatest.{BeforeAndAfterAll, ConfigMap, FunSuite}

import scala.collection.immutable.HashMap

class TestSuiteTests extends FunSuite with BeforeAndAfterAll {

  val testModelFile: File = new File("../dump/src/test/resources/new_release_based_ci_tests_draft.ttl")

  val testModel: Model = ModelFactory.createDefaultModel()

//  override def beforeAll(configMap: ConfigMap): Unit = {
//
//
//    RDFDataMgr.read(testModel, new FileInputStream(testModelFile),RDFLanguages.TURTLE)
//  }

  test("ValidationExecutor") {

    val hadoopHomeDir = new File("./.haoop/")
    hadoopHomeDir.mkdirs()
    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)

    val sparkSession = SparkSession.builder()
      .config("hadoop.home.dir", "./.hadoop")
      .config("spark.local.dir", "./.spark")
      .appName("Test Iris").master("local[*]").getOrCreate()
    sparkSession.sparkContext.setLogLevel("WARN")

    val sqlContext: SQLContext = sparkSession.sqlContext


//    ValidationExecutor.testIris(testModel,)(sqlContext)
  }

  test("Array") {

    val array = Array.fill[Long](10)(0)

    array(0) = 1

    array.foreach(println(_))
  }

  test("Dataype Pattern") {

    val literal = "\"2019-05-21\"^^<http://www.w3.org/2001/XMLSchema#date> "

    val patternString = "^-?([1-9][0-9]{3,}|0[0-9]{3})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?$"

    val lexicalForm = literal.trim.split("\"").dropRight(1).drop(1).mkString("")

    println(lexicalForm)
  }

  test( "Map Test") {

    val immumap = HashMap(
      Array("a" -> 1, "b" -> 2, "a" -> 2).groupBy(_._1).map(entry => entry._1 -> entry._2.map(_._2)).toArray: _*
    )

    immumap("a").foreach(print)

  }
  test("Ntriple Test" ) {

    val model = ModelFactory.createDefaultModel()

    val line = "<#a> <#b> \"abc\"@en ."

    val tripleIter = RDFDataMgr.createIteratorTriples(new ByteArrayInputStream(line.getBytes("UTF-8")),RDFLanguages.NTRIPLES,"urn:base")

    println(
      tripleIter.next().getObject.getLiteralDatatype
    )
  }

//  test( "RDF Evaluation Report" ) {
//
//    val model = ModelFactory.createDefaultModel()
//
//    val label : String = "Subjects"
//    val testReport: TestReport = TestReport(0,0,Array[Long](),Array[Long]())
//    val triggerCollection: Array[Trigger] = Array[Trigger]()
//    val testApproachCollection: Array[TestApproach] = Array[TestApproach]()
//
//    triggerCollection.foreach(
//
//      trigger => {
//
////        model.add(ResourceFactory.createStatement(
////          //        ResourceFactory.createResource()
////        ))
//
//        trigger.testCases
//
//      }
//    )
//
//    model.write(System.out,"Turtle")
//  }
}
