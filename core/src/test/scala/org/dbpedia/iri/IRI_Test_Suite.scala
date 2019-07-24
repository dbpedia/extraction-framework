package org.dbpedia.iri

import java.io.{ByteArrayOutputStream, File}

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{ModelFactory, RDFWriter, ResourceFactory}
import org.apache.jena.riot.{RDFFormat, RDFLanguages}
import org.apache.jena.riot.system.{IRIResolver, StreamRDFWriter}
import org.apache.spark.sql.{Dataset, SparkSession}
import org.scalatest.FunSuite
import org.dbpedia.validation

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import scala.io.{Codec, Source}

class IRI_Test_Suite  extends FunSuite{


  test("Trigger Test") {
    /*
    TODO
     */
  }

  test("Test Case Query") {

    val pathToTestCases = "../new_release_based_ci_tests_draft.nt"

    val model = ModelFactory.createDefaultModel()
    model.read(pathToTestCases)

    val query = QueryFactory.create(org.dbpedia.validation.testQuery())
    val queryExecutionFactory = QueryExecutionFactory.create(query,model)

    val resultSet = queryExecutionFactory.execSelect()

    val vars = resultSet.getResultVars

    while( resultSet.hasNext ) {
      val solution = resultSet.next()
      vars.foreach(v => println(solution.get(v)))
    }
  }

  test("Spark Approach") {

    val hadoopHomeDir = new File("./haoop/")
    hadoopHomeDir.mkdirs()
    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)
    System.setProperty("log4j.logger.org.apache.spark.SparkContext", "WARN")

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

    val sparkSession = SparkSession.builder().config("hadoop.home.dir", "./hadoop")
      .appName("Dev 3").master("local[*]").getOrCreate()

    //    sparkSession.sparkContext.setLogLevel("WARN")

    val sqlContext = sparkSession.sqlContext
    import sqlContext.implicits._

    val rdd = sqlContext.createDataset(extractionOutputTtl.lines.toSeq)

    val counts = rdd.map(line => {

      val spo = line.split(" ", 3)

      //      implicit def betterStringConversion(str: String) = new BetterString(str)

      var s: String = null
      if (spo(0).startsWith("<")) {
        s = spo(0).substring(1, spo(0).length - 1)
      }

      //  var tS, vS, tP, vP, tO, vO: Long = 0L
      //
      var p: String = null
      if (spo(1).startsWith("<")) {
        p = spo(1).substring(1, spo(1).length - 1)
      }

      var o: String = null
      if (spo(2).startsWith("<")) {
        o = spo(2).substring(1, spo(2).length - 3)
      }

      println(s)
      SPO(s,p,o)
    }).map(_.s).distinct().filter(_ != null).map( x => ReduceScore(1,1,0) )
      .reduce( (a,b) => ReduceScore(a.cntAll+b.cntAll,a.cntTrigger+b.cntTrigger,a.cntValid+b.cntValid))

    println(counts.cntAll)
    println(counts.cntTrigger)
    println(counts.cntValid)


  }
  case class RawRdfTripleParts()

  case class IriScore(tS: Long , vS: Long, tP: Long, vP: Long, tO: Long, vO:Long)

  implicit  class FlatRdfTriplePart(s: String) {
    def checkIsIri: Boolean = s.startsWith("<")
  }

  test("Single Iri Parse Test") {

//    IRIResolver.iriFactory.construct("http://dbpedia.org/>/test")
  }
}

case class ReduceScore(cntAll: Long, cntTrigger: Long, cntValid: Long)
case class SPO(s: String, p: String, o: String)