package org.dbpedia.extraction.util

import org.dbpedia.iri.{IRI, UriUtils}
import org.scalatest.FunSuite

/**
  * Created by Termilion on 13.03.2017.
  */
class UriUtils$IriTest extends FunSuite {
  val log = true

  test("simple non-ASCII characters") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/Robert_Sch%C3%B6ller?abc=123"
    val resultIri = "http://dbpedia.org/resource/Robert_Schöller?abc=123"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("double + instead of whitespace") {
    // should be decoded completely
    val testUri = "http://dbpedia.org/resource/Jeanne  Deroubaix"
    val resultIri = "http://dbpedia.org/resource/Jeanne_Deroubaix"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
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
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("query test") {
    // should be decoded completely, special: direction change
    val testUri = "http://dbpedia-live.openlinksw.com/sparql/?default-graph-uri=http%3A%2F%2Fstatic.dbpedia.org&qtxt=describe+%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FAmsterdam%3E&format=text%2Fx-html%2Bul&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on"
    val resultIri = "http://dbpedia-live.openlinksw.com/sparql/?default-graph-uri=http://static.dbpedia.org&qtxt=describe+%3Chttp://dbpedia.org/resource/Amsterdam%3E&format=text/x-html+ul&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("simple test") {
    // should be decoded completely, special: direction change
    val testUri = "http://www.example.org/red%09ros%C3%A9#red"
    val resultIri = "http://www.example.org/red\trosé#red"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("simple testNotSoSimple") {
    // should be decoded completely, special: direction change
    val testUri = "http://example.com/%F0%90%8C%80%F0%90%8C%81%F0%90%8C%82"
    val resultIri = "http://example.com/\uD800\uDF00\uD800\uDF01\uD800\uDF02"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("chinese") {
    // should be decoded completely, special: direction change
    val testUri = "http://example.com/base/%E6%A4%8D%E7%89%A9%2F%E5%90%8D%3D%E3%81%97%E3%81%9D%3B%E4%BD%BF%E7%94%A8%E9%83%A8%3D%E8%91%89"
    val resultIri = "http://example.com/base/植物/名=しそ;使用部=葉"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("testx1") {
    // should be decoded completely, special: direction change
    val testUri = "http://iris.test.ing/foo%3fbar"
    val resultIri = "http://iris.test.ing/foo?bar"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }

  test("testx3") {
    // should be decoded completely, special: direction change
    val testUri = "http://foo/path;a??e#f#g"
    val resultIri = "http://foo/path;a??e#f#g"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }


  test("testx2") {
    // should be decoded completely, special: direction change
    val testUri = "http://foo/abcd#foo?bar"
    val resultIri = "http://foo/abcd#foo?bar"
    val decoded = UriUtils.uriToIri(testUri)
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + decoded)
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
    assert(decoded.getFragment == "foo?bar", "Decoded: " +decoded.getFragment + " does not equal Expected: " + "foo?bar")

  }


  test("testx4") {
    // should be decoded completely, special: direction change
    val testUri = "http://user:pass@foo:21/bar;par?b#c"
    val resultIri = "http://user:pass@foo:21/bar;par?b#c"
    val decoded = UriUtils.uriToIri(testUri)
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + decoded)
      info("Exp.IRI: " + resultIri)
    }

    val res = UriUtils.uriToIri(testUri)
    assert(res.equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
    assert(decoded.getFragment == "c" && decoded.getQuery == "b" && decoded.getPath == "/bar;par" && decoded.getUserinfo == "user:pass", "zonk")

  }

  test("dbpedia uri") {
    // should be decoded completely
    val testUri = "http://nl.dbpedia.org/resource/Ren%C3%A9_Descartes"
    val resultIri = "http://nl.dbpedia.org/resource/René_Descartes"
    if (log == true) {
      info("TestURI: " + testUri)
      info("Decoded: " + UriUtils.uriToIri(testUri))
      info("Exp.IRI: " + resultIri)
    }
    assert(UriUtils.uriToIri(testUri).equals(IRI.create(resultIri)), "Decoded: " + UriUtils.uriToIri(testUri) + " does not equal Expected: " + resultIri)
  }
}
