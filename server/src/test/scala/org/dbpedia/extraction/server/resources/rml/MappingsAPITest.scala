package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by wmaroy on 09.08.17.
  */
class MappingsAPITest extends FlatSpec with Matchers {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  SERVER MUST BE RUNNING AT LOCALHOST:9999
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  "POST /server/rml/mappings" should "work without errors" in {
    val resource = "/mappingsAPITest/resource.json"
    val expected = "/mappingsAPITest/expected.json"
    val tuple = postTest(resource, expected)
    assert(tuple._1 == tuple._2)
  }

  private def postTest(resource : String, expected : String) : (String, String) = {

    val stream : InputStream = getClass.getResourceAsStream(resource)
    val json = scala.io.Source.fromInputStream( stream ).getLines.mkString

    val stream2 : InputStream = getClass.getResourceAsStream(expected)
    val expectedJson = scala.io.Source.fromInputStream( stream2 ).getLines.mkString

    val httpClient = new DefaultHttpClient()
    val requestEntity = new StringEntity(
      json,
      ContentType.APPLICATION_JSON)

    val postMethod = new HttpPost("http://localhost:9999/server/rml/mappings/")
    postMethod.setEntity(requestEntity)
    val rawResponse = httpClient.execute(postMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    (responseString, expectedJson)

  }

}
