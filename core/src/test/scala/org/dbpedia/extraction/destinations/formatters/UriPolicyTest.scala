package org.dbpedia.extraction.destinations.formatters

import java.net.URI

import org.junit.runner.RunWith
import org.scalatest.{Matchers, FlatSpec}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UriPolicyTest extends FlatSpec with Matchers {

  "toUri" should "return http://example.com" in
    {
      prepareToUri("http://example.com") should equal ("http://example.com")
    }

  "toUri" should "return http://xn--detrsbonsdomaines-vsb.com" in
    {
      prepareToUri("http://detrèsbonsdomaines.com") should equal ("http://xn--detrsbonsdomaines-vsb.com")
    }

  // port
  "toUri" should "return http://xn--detrsbonsdomaines-vsb.com:88" in
    {
      prepareToUri("http://detrèsbonsdomaines.com:88") should equal ("http://xn--detrsbonsdomaines-vsb.com:88")
    }

  // port & user
  "toUri" should "return http://dbpedia@xn--detrsbonsdomaines-vsb.com:88" in
    {
      prepareToUri("http://dbpedia@detrèsbonsdomaines.com:88") should equal ("http://dbpedia@xn--detrsbonsdomaines-vsb.com:88")
    }

  "toUri" should "return http://dbpedia@xn--detrsbonsdomaines-vsb.com:88/dbpedia" in
    {
      prepareToUri("http://dbpedia@detrèsbonsdomaines.com:88/dbpedia") should equal ("http://dbpedia@xn--detrsbonsdomaines-vsb.com:88/dbpedia")
    }

  "toUri" should "return http://dbpedia@xn--detrsbonsdomaines-vsb.com:88/dbp%C3%ABdia" in
    {
      prepareToUri("http://dbpedia@detrèsbonsdomaines.com:88/dbpëdia") should equal ("http://dbpedia@xn--detrsbonsdomaines-vsb.com:88/dbp%C3%ABdia")
    }

  private def policyApplicableMock(uri: String ): Boolean = {true}

  private def prepareToUri(uri: String) : String = {
    UriPolicy.toUri(new URI(uri)).toString
  }

}
