package org.dbpedia.validation

import java.io.{File, FileInputStream}

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.scalatest.{BeforeAndAfterAll, ConfigMap, FunSuite}

class TestSuiteTests extends FunSuite with BeforeAndAfterAll {

  val testModelFile: File = new File("../dump/src/test/resources/new_release_based_ci_tests_draft.ttl")

  val testModel: Model = ModelFactory.createDefaultModel()

  override def beforeAll(configMap: ConfigMap): Unit = {


    RDFDataMgr.read(testModel, new FileInputStream(testModelFile),RDFLanguages.TURTLE)
  }

  test("Load_Validator_Test") {

    TestSuiteFactory.loadIriValidatorsDev(testModel).foreach( validator => {
      println(validator.id)
      println(validator.doesNotContains)
    })

    //testModel.write(System.out,"Turtle")
  }

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
}
