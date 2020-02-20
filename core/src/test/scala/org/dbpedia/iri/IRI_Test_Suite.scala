package org.dbpedia.iri

import java.io.File

import org.apache.jena.iri.IRIException
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.system.IRIResolver
import org.scalatest.FunSuite

import scala.collection.JavaConversions._
import scala.util.matching.Regex


case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
case class SPO(s: String, p: String, o: String)

class IRI_Test_Suite  extends FunSuite{


  test("Trigger Test") {
    /*
    TODO
     */
  }


  test("Spark Approach") {

//    TODO rework
//    val hadoopHomeDir = new File("./haoop/")
//    hadoopHomeDir.mkdirs()
//    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)
//    System.setProperty("log4j.logger.org.apache.spark.SparkContext", "WARN")
//
//    val extractionOutputTtl =
//      s"""
//         |<http://commons.dbpedia.org/resource/File:Rainbow_Sopot_Poland_2004_08_11_ubt.jpeg> <http://dbpedia.org/ontology/license> <http://commons.dbpedia.org/resource/http://creativecommons.org/licenses/by/2.5/> .
//       """.stripMargin.trim
//
//    val sparkSession = SparkSession.builder().config("hadoop.home.dir", "./hadoop")
//      .appName("Dev 3").master("local[*]").getOrCreate()
//
//    //    sparkSession.sparkContext.setLogLevel("WARN")
//
//    val sqlContext = sparkSession.sqlContext
//    import sqlContext.implicits._
//
//    val rdd = sqlContext.createDataset(extractionOutputTtl.lines.toSeq)
//
//    val counts = rdd.map(line => {
//
//      val spo = line.split(" ", 3)
//
//      //      implicit def betterStringConversion(str: String) = new BetterString(str)
//
//      var s: String = null
//      if (spo(0).startsWith("<")) {
//        s = spo(0).substring(1, spo(0).length - 1)
//      }
//
//      //  var tS, vS, tP, vP, tO, vO: Long = 0L
//      //
//      var p: String = null
//      if (spo(1).startsWith("<")) {
//        p = spo(1).substring(1, spo(1).length - 1)
//      }
//
//      var o: String = null
//      if (spo(2).startsWith("<")) {
//        o = spo(2).substring(1, spo(2).length - 3)
//      }
//
//      println(s)
//      SPO(s,p,o)
//    }).map(_.s).distinct().filter(_ != null).map( x => ReduceScore(1,1,0) )
//      .reduce( (a,b) => ReduceScore(a.cntAll+b.cntAll,a.cntTrigger+b.cntTrigger,a.cntValid+b.cntValid))
//
//    println(counts.cntAll)
//    println(counts.cntTrigger)
//    println(counts.cntValid)

  }
  case class RawRdfTripleParts()

  case class IriScore(tS: Long , vS: Long, tP: Long, vP: Long, tO: Long, vO:Long)

  implicit  class FlatRdfTriplePart(s: String) {
    def checkIsIri: Boolean = s.startsWith("<")
  }

  test("Single Iri Parse Test") {

    try {
      IRIResolver.iriFactory.construct("http://dbpedia.org/>/test")
    }
    catch {
      case iriex: IRIException => println("Invalid IRI definition")
    }
  }

  test("Iri Trigger Pattern Test") {

    val rawPattern = "^http://(ar\\.|az\\.|be\\.|bg\\.|bn\\.|ca\\.|cs\\.|cy\\.|da\\.|de\\.|el\\.|en\\.|eo\\.|es\\.|et\\.|eu\\.|fa\\.|fi\\.|fr\\.|ga\\.|gl\\.|hi\\.|hr\\.|hu\\.|hy\\.|id\\.|it\\.|ja\\.|ko\\.|lt\\.|lv\\.|mk\\.|nl\\.|pl\\.|pt\\.|ro\\.|ru\\.|sk\\.|sl\\.|sr\\.|sv\\.|tr\\.|uk\\.|ur\\.|vi\\.|war\\.|zh\\.|commons\\.)?dbpedia.org/resource/"
    val pattern = rawPattern.r

    val iri = "http://ar.dbpedia.org/resource/"
    val iri1 = "https://ar.dbpedia.org/resource/"
    val iri2 = "http://ra.dbpedia.org/resource/"

    println(pattern.pattern.matcher(iri).matches())
    println(pattern.pattern.matcher(iri1).matches())
    println(pattern.pattern.matcher(iri2).matches())
  }

  test(" Iri Validator Pattern Test") {

//    import org.dbpedia.validation.IriValidator
//
//    val validator = IriValidator("","",true,true,Array('#','&'))
//
//    val validIri = "http://a.b/c%26d%23e"
//    val nonValidIri = "http://a.b/c&d#e"
//
//    val validatorPatternStr =  s"[${validator.notContainsChars.mkString("")}]"
//    println(validatorPatternStr)
//    val validatorRegex = validatorPatternStr.r
//
//    if ( validatorRegex.findAllIn(validIri).length < 1 ) println(validIri+" is valid")
//    else println(validIri+" is not valid")
//
//    if ( validatorRegex.findAllIn(nonValidIri).length < 1 ) println(nonValidIri+" is valid")
//    else println(nonValidIri+" is not valid")
  }

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

