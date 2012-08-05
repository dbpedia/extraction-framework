package org.dbpedia.extraction.mappings

import java.lang.String
import collection.mutable._
import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.utils.sparql.{CachingGraphDAO, HTTPGraphDAO}
import org.dbpedia.utils.sparql.SparqlUtils
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.util.Language
object AugmenterExtractorTest
{
  def main(args : Array[String]) : Unit = {
    val inst = new AugmenterExtractorTest()

    inst.runTest()
  }

}

class AugmenterExtractorTest extends FlatSpec with ShouldMatchers
{
  /*
   "AugmenterExtractor" should "succeed without errors" in {
      runTest()
     1 should equal (1)
   }
   */


  private def runTest() : Unit = {
    val tmp = new HTTPGraphDAO("http://hanne.aksw.org:8892/sparql", Some("http://dbpedia.org"))

    val graphDAO = new CachingGraphDAO(tmp, "/tmp/sparqlcache")

    val labelToURIs = SparqlUtils.getInstancesUriAndLabels(graphDAO, "http://dbpedia.org/ontology/Country", "en")

    val context = new
    {
        def language : Language = Language.English
    }

    val decoratee = new DummyExtractor(context)



    val canon = new HashMap[String, Set[String]] with MultiMap[String, String]
    labelToURIs.foreach(e => e._2.foreach(x => canon.addBinding(AugmenterExtractorUtils.canonicalize(e._1), x)))

    println(canon)

    val extractor = new AugmenterExtractor(decoratee, new Dataset("enriched"), canon, "http://dbpedia.org/ontology/geoRelated")

    val graph = extractor.extract(null, "http://subjectPageURI", null)

    println(graph)
  }



}