package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by wmaroy on 14.08.17.
  */
class ExtractionAPITest extends FlatSpec with Matchers {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  SERVER MUST BE RUNNING AT LOCALHOST:9999
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Test cases
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  "POST /extract with simple property templates that is already loaded in memory" should "work properly" in {

    val result = postTest("/extractSimpleTest/input.json", "/extractSimpleTest/output.json")

    result._1.split("\n").length should be > 20

  }

  "POST /extract with simple property templates that is not loaded in memory" should "work properly" in {

    val result = postTest("/extractNotLoadedSimpleTest/input.json", "/extractNotLoadedSimpleTest/output.json")

    //result._1.split("\n").length should be > 20

  }

  "POST /extract with conditional templates" should "work properly" in {


  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private method
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def postTest(resource : String, expected : String) : (String, String) = {

    val stream : InputStream = getClass.getResourceAsStream(resource)
    val json = scala.io.Source.fromInputStream( stream ).getLines.mkString

    val stream2 : InputStream = getClass.getResourceAsStream(expected)
    val expectedJson = scala.io.Source.fromInputStream( stream2 ).getLines.mkString

    val httpClient = new DefaultHttpClient()
    val requestEntity = new StringEntity(
      json,
      ContentType.APPLICATION_JSON)

    val postMethod = new HttpPost("http://localhost:9999/server/rml/extract")
    postMethod.setEntity(requestEntity)
    val rawResponse = httpClient.execute(postMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    val mapper = new ObjectMapper()
    val responseNode = mapper.readTree(responseString)
    val text = responseNode.get("dump").asText()

    println("\n\n" + text
    )
    (text, expectedJson)

  }

}
