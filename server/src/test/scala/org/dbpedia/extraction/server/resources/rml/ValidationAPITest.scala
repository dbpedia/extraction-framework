package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 17.08.17.
  */
class ValidationAPITest extends FunSuite {


  test("Valid mappings should give valid result") {

    val tuple = postTest("/validationAPITest/test_1/input.json",
      "/validationAPITest/test_1/output.json")

    assert(tuple._1.equals(tuple._2))

  }


  test("Invalid mappings should give invalid result") {

    val tuple = postTest("/validationAPITest/test_2/input.json",
      "/validationAPITest/test_2/output.json")

    assert(tuple._1.equals(tuple._2))

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

    val postMethod = new HttpPost("http://localhost:9999/server/rml/validate/")
    postMethod.setEntity(requestEntity)
    val rawResponse = httpClient.execute(postMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    (responseString, expectedJson)

  }

}


