package org.dbpedia.extraction.util

import java.net.{URI, URLEncoder}

import org.dbpedia.util.text.uri.UriToIriDecoder
import org.scalatest.FunSuite

/**
  * Created by Termilion on 13.03.2017.
  */
class UriUtils$IriTest extends FunSuite {
  val log = true

  test("simple non-ASCII characters") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/Robert_Sch%C3%B6ller"
    val resultIri = "http://dbpedia.org/resource/Robert_Schöller"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("reserved characters") {
    // !#$&'()*+,/:;=?@[] should stay encoded
    val testUri = "http://dbpedia.org/resource/%21%23%24%26%27%28%29%2A%2B%2C%2F%3A%3B%3D%3F%40%5B%5D"
    val resultIri = "http://dbpedia.org/resource/%21%23%24%26%27%28%29%2A%2B%2C%2F%3A%3B%3D%3F%40%5B%5D"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("unwise characters and double whitespace") {
    // "<>[\]^`{|} should be re-encoded and double whitespace gets replaced with one underscore
    val testUri = "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C%7D  test"
    val resultIri = "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C%7D_test"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("russian characters") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/%D1%84%D0%BB%D1%8D%D1%88%D0%B1%D0%B5%D0%BA%D0%B0%D1%85"
    val resultIri = "http://dbpedia.org/resource/флэшбеках"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("encoding-depth > 1") {
    // should be decoded completely
    val testUri = "http://pt.dbpedia.org/resource/%25C3%2581rea_de_Re…"
    val resultIri = "http://pt.dbpedia.org/resource/Área_de_Re…"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("invalid Escape Sequence: too short") {
    // should not throw an error, should just be ignored
    val testUri = "http://pt.dbpedia.org/resource/foo%3"
    val resultIri = "http://pt.dbpedia.org/resource/foo%3"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("invalid Escape Sequence: not hexadecimal") {
    // should not throw an error, should just be ignored
    val testUri = "http://pt.dbpedia.org/resource/foo%2K"
    val resultIri = "http://pt.dbpedia.org/resource/foo%2K"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

  test("arabic characters") {
    // should be decoded completely, special: direction change
    val testUri = "http://pt.dbpedia.org/resource/%D8%AA%D9%85%D8%AA%D9%84%D9%83"
    val resultIri = "http://pt.dbpedia.org/resource/تمتلك"
    if (log == true) info(UriUtils.uriToIri(testUri))
    assert(UriUtils.uriToIri(testUri).equals(resultIri))
  }

}
