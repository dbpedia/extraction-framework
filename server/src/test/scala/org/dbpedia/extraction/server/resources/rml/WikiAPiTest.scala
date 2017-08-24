package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.scalatest.{FlatSpec, Matchers}

class WikiAPiTest extends FlatSpec with Matchers {

  "GET server/rml/en/wiki/template" should "work correctly" in {

    postTest("/wikiApiTest/templatesTest/input.json", "/wikiApiTest/templatesTest/output.json")

  }

  private def postTest(resource : String, expected : String) : (String, String) = {

    val stream : InputStream = getClass.getResourceAsStream(resource)
    val json = scala.io.Source.fromInputStream( stream ).getLines.mkString

    val stream2 : InputStream = getClass.getResourceAsStream(expected)
    val expectedJson = scala.io.Source.fromInputStream( stream2 ).getLines.mkString

    val httpClient = new DefaultHttpClient()
    val getMethod = new HttpGet("http://localhost:9999/server/rml/en/wiki/Bill_Gates/templates")
    val rawResponse = httpClient.execute(getMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    (responseString, expectedJson)

  }

}
