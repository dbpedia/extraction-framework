package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by wmaroy on 09.08.17.
  */
class StatisticsAPITest extends FlatSpec with Matchers {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  SERVER MUST BE RUNNING AT LOCALHOST:9999
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  "GET /server/rml/en/statistics" should "work without errors" in {
    val tuple = getTest()
  }

  private def getTest() : String = {


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
