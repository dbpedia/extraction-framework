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
class TemplateAssemblerTest extends FlatSpec with Matchers {

  // Server must be running!

  "SimplePropertyTemplates" should "be generated correctly" in {

    val tuple = TemplateTestUtil.postTest("/simplePropertyTemplateTest/simplePropertyTemplate.json",
                              "/simplePropertyTemplateTest/expected_simplePropertyTemplate.json",
                              "simpleproperty")


    assert(tuple._1.equals(tuple._2))

  }

  "ConstantTemplates" should "be generated correctly" in {

    val tuple = TemplateTestUtil.postTest("/constantTemplateTest/constantTemplate.json",
      "/constantTemplateTest/expected_constantTemplate.json",
      "constant")

    assert(tuple._1.equals(tuple._2))

  }

  "GeocoordinateTemplates" should "be generated correctly" in {

    val tuple = TemplateTestUtil.postTest("/geocoordinateTemplateTest/geocoordinateTemplate.json",
      "/geocoordinateTemplateTest/expected_geocoordinateTemplate.json",
      "geocoordinate")

    assert(tuple._1.equals(tuple._2))

  }

}
