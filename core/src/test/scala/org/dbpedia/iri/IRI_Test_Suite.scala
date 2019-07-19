package org.dbpedia.iri

import org.apache.spark.sql.SparkSession
import org.scalatest.FunSuite

class IRI_Test_Suite  extends FunSuite{

  test("Trigger Test") {

//    load_iri_list("")
    load_test_cases("../new_release_based_ci_tests_draft.nt")

//    val spark_session = SparkSession.builder().appName("IRI Tests").master("local[*]").getOrCreate()
//    val spark_context = spark_session.sparkContext
//    spark_context.setLogLevel("WARN")

//    TODO

  }
}
