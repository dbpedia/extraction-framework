package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.{FlatSpec, Matchers}
import scala.collection.JavaConverters._

/**
  * Created by wmaroy on 09.08.17.
  */
class StatisticsAPITest extends FlatSpec with Matchers {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  SERVER MUST BE RUNNING AT LOCALHOST:9999
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  "GET /server/rml/en/statistics" should "work without errors" in {
    val tuple = getTest
  }

  "GET /server/rml/en/statistics" should "not contain a mapping stat that has over 100% mapping ratio" in {
    val response = getTest

    val mapper = new ObjectMapper()
    val responseNode = mapper.readTree(response)
    val stats = responseNode.get("statistics").elements().asScala

    // check all stats
    assert(!stats.exists(_.get("mappedRatio").asInt() > 100))

  }

  private def getTest: String = {


    val httpClient = new DefaultHttpClient()

    val getMethod = new HttpGet("http://localhost:9999/server/rml/en/statistics")
    val rawResponse = httpClient.execute(getMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    responseString

  }

}
