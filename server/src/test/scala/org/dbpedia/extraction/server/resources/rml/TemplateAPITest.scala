package org.dbpedia.extraction.server.resources.rml

import java.io.InputStream

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicResponseHandler, DefaultHttpClient}
import org.apache.http.util.EntityUtils
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by wmaroy on 25.07.17.
  */
class TemplateAPITest extends FlatSpec with Matchers {

  // Server must be running!

  "SimplePropertyTemplates" should "be generated correctly" in {

    val tuple = postTest("/simplePropertyTemplateTest/simplePropertyTemplate.json",
                              "/simplePropertyTemplateTest/expected_simplePropertyTemplate.json",
                              "simpleproperty")


    assert(tuple._1.equals(tuple._2))

  }

  "ConstantTemplates" should "be generated correctly" in {

    val tuple = postTest("/constantTemplateTest/constantTemplate.json",
      "/constantTemplateTest/expected_constantTemplate.json",
      "constant")

    assert(tuple._1.equals(tuple._2))

  }

  "GeocoordinateTemplates" should "be generated correctly" in {

    val tuple = postTest("/geocoordinateTemplateTest/geocoordinateTemplate.json",
      "/geocoordinateTemplateTest/expected_geocoordinateTemplate.json",
      "geocoordinate")

    assert(tuple._1.equals(tuple._2))

  }

  "StartDateTemplates" should "be generated correctly" in {

    val tuple = postTest("/startDateTemplateTest/template.json",
      "/startDateTemplateTest/expected_response.json",
      "startdate")

    assert(tuple._1.equals(tuple._2))

  }

  "EndDateTemplates" should "be generated correctly" in {

    val tuple = postTest("/endDateTemplateTest/template.json",
      "/endDateTemplateTest/expected_response.json",
      "enddate")

    assert(tuple._1.equals(tuple._2))

  }

   private def postTest(resource : String, expected : String, template : String) : (String, String) = {

    val stream : InputStream = getClass.getResourceAsStream(resource)
    val json = scala.io.Source.fromInputStream( stream ).getLines.mkString

    val stream2 : InputStream = getClass.getResourceAsStream(expected)
    val expectedJson = scala.io.Source.fromInputStream( stream2 ).getLines.mkString

    val httpClient = new DefaultHttpClient()
    val requestEntity = new StringEntity(
      json,
      ContentType.APPLICATION_JSON)

    val postMethod = new HttpPost("http://localhost:9999/server/rml/templates/" + template)
    postMethod.setEntity(requestEntity)
    val rawResponse = httpClient.execute(postMethod)
    val responseString = new BasicResponseHandler().handleResponse(rawResponse)

    println("++++ Response body:")
    println(responseString)
    println("++++")

    (responseString, expectedJson)

  }

}
