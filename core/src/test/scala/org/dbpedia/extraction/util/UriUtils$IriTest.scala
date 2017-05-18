package org.dbpedia.extraction.util

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
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("+ character instead of %20") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/Jeanne+Deroubaix"
    val resultIri = "http://dbpedia.org/resource/Jeanne_Deroubaix"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("reserved characters") {
    // ?#[|] should stay encoded, rest of reserved chars schould be decoded
    val testUri = "http://dbpedia.org/resource/%21%23%3F%5B%5D%7D%2A"
    val resultIri = "http://dbpedia.org/resource/!%23%3F%5B%5D%7D*"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("unwise characters and double whitespace") {
    // "<>[\]^`{|} should be re-encoded and double whitespace gets replaced with one underscore
    val testUri = "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C  test"
    val resultIri = "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C_test"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("double + instead of whitespace") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/Jeanne++Deroubaix"
    val resultIri = "http://dbpedia.org/resource/Jeanne_Deroubaix"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("russian characters") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/%D1%84%D0%BB%D1%8D%D1%88%D0%B1%D0%B5%D0%BA%D0%B0%D1%85"
    val resultIri = "http://dbpedia.org/resource/флэшбеках"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("encoding-depth > 1") {
    // should be decoded completely
    val testUri = "http://pt.dbpedia.org/resource/%25C3%2581rea_de_Re…"
    val resultIri = "http://pt.dbpedia.org/resource/Área_de_Re…"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("invalid Escape Sequence: too short") {
    // should not throw an error, should just be ignored
    val testUri = "http://pt.dbpedia.org/resource/foo%3"
    val resultIri = "http://pt.dbpedia.org/resource/foo%3"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("invalid Escape Sequence: not hexadecimal") {
    // should not throw an error, should just be ignored
    val testUri = "http://pt.dbpedia.org/resource/foo%2K"
    val resultIri = "http://pt.dbpedia.org/resource/foo%2K"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("arabic characters") {
    // should be decoded completely, special: direction change
    val testUri = "http://pt.dbpedia.org/resource/%D8%AA%D9%85%D8%AA%D9%84%D9%83"
    val resultIri = "http://pt.dbpedia.org/resource/تمتلك"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(resultIri), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

}
