package org.dbpedia.validation

import java.io.File

import org.apache.spark.sql.SparkSession

object ValidationLauncher {

  def main(args: Array[String]): Unit = {
    val hadoopHomeDir = new File("./haoop/")
    hadoopHomeDir.mkdirs()
    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)
//    System.setProperty("log4j.logger.org.apache.spark.SparkContext", "WARN")

    val extractionOutputTtl =
      s"""
         |<http://wikidata.dbpedia.org/resource/Q15> <http://www.georss.org/georss/point> "1.0 17.0" .
         |<http://wikidata.dbpedia.org/resource/Q15> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> .
         |<http://wikidata.dbpedia.org/resource/Q15> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "1.0"^^<http://www.w3.org/2001/XMLSchema#float> .
         |<http://wikidata.dbpedia.org/resource/Q15> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "17.0"^^<http://www.w3.org/2001/XMLSchema#float> .
         |<http://wikidata.dbpedia.org/resource/Q21> <http://www.georss.org/georss/point> "53.0 -1.0" .
         |<http://wikidata.dbpedia.org/resource/Q21> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing> .
         |<http://wikidata.dbpedia.org/resource/Q21> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> "53.0"^^<http://www.w3.org/2001/XMLSchema#float> .
         |<http://wikidata.dbpedia.org/resource/Q21> <http://www.w3.org/2003/01/geo/wgs84_pos#long> "-1.0"^^<http://www.w3.org/2001/XMLSchema#float> .
         |<http://wikidata.dbpedia.org/resource/Q18> <http://www.georss.org/georss/point> "-21.0 -59.0" .
       """.stripMargin.trim

    val sparkSession = SparkSession.builder().config("hadoop.home.dir","./hadoop")
      .appName("Dev 3").master("local[*]").getOrCreate()

    val rdd = sparkSession.sparkContext.parallelize(extractionOutputTtl.lines.toSeq)

    rdd.fold("")( (a,b) => a + b ).foreach(print)
  }

//  def main(args: Array[String]): Unit = {
//
//    val pathToTestCaseFile: String = args(0)
//    val pathToFlatTurtleFile: String = args(1)
//
//    val sparkSession = SparkSession.builder().master("local[*]").appName("Rdf Validation").getOrCreate()
//    sparkSession.sparkContext.setLogLevel("WARN")
//
//    val untestedFlatTurtle = sparkSession.sqlContext.read.textFile(pathToFlatTurtleFile)
//
//    //TODO skip \s and check then
//    untestedFlatTurtle.filter(! _.startsWith("#"))
//
//
//
//
//  }

//  //    load_iri_list("")
//  load_test_cases("../new_release_based_ci_tests_draft.nt")
//
//  //    val spark_session = SparkSession.builder().appName("IRI Tests").master("local[*]").getOrCreate()
//  //    val spark_context = spark_session.sparkContext
//  //    spark_context.setLogLevel("WARN")
//
//  //    TODO
//
//}

//test("Another Test") {
//
//
//  val m_tests = ModelFactory.createDefaultModel()
//  m_tests.read("../new_release_based_ci_tests_draft.nt")
//
//  val q_validator = QueryFactory.create(
//
//  s"""
//         |PREFIX v: $prefix_v
//         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
//         |
//         |SELECT ?validator ?hasScheme ?hasQuery ?hasFragment (group_concat(?notContain; SEPARATOR="\t") as ?notContains) {
//         |  ?validator
//         |     a                          v:IRI_Validator ;
//         |     v:hasScheme                ?hasScheme ;
//         |     v:hasQuery                 ?hasQuery ;
//         |     v:hasFragment              ?hasFragment ;
//         |     v:doesNotContainCharacters ?notContain .
//         |
//         |} GROUP BY ?validator ?hasScheme ?hasQuery ?hasFragment
//      """.stripMargin)
//
//  val query_exec = QueryExecutionFactory.create(q_validator, m_tests)
//  val result_set = query_exec.execSelect()
//
//  val l_iri_validator = ListBuffer[IRI_Validator]()
//
//  while (result_set.hasNext) {
//
//  val solution = result_set.next()
//
//  print(
//  s"""
//           |FOUND VALIDATOR: ${solution.getResource("validator").getURI}
//           |> SCHEME: ${solution.getLiteral("hasScheme").getLexicalForm}
//           |> QUERY: ${solution.getLiteral("hasQuery").getLexicalForm}
//           |> FRAGMENT: ${solution.getLiteral("hasFragment").getLexicalForm}
//           |> NOT CONTAIN: ${List(solution.getLiteral("notContains").getLexicalForm)}
//        """.stripMargin
//  )
//}
}
