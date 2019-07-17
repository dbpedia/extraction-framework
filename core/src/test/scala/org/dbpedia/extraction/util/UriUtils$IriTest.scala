package org.dbpedia.extraction.util

import org.dbpedia.iri.{IRI, UriUtils}
import org.scalatest.FunSuite

/**
  * Created by Termilion on 13.03.2017.
  */
class UriUtils$IriTest extends FunSuite {
  val log = true

  val testList: List[(String, String, String)] = List(

    /**
      * simple non-ASCII characters
      */

    ("%C3%B6 -> ö, simple non-ASCII characters",
      // should be decoded completely
      "http://dbpedia.org/resource/Robert_Sch%C3%B6ller?abc=123",
      "http://dbpedia.org/resource/Robert_Schöller?abc=123"),


    ("%C3%A9 -> é, simple non-ASCII characters",
      // should be decoded completely
      "http://nl.dbpedia.org/resource/Ren%C3%A9_Descartes",
      "http://nl.dbpedia.org/resource/René_Descartes"),

    ("russian, simple non-ASCII characters",
      // should be decoded completely
      "http://dbpedia.org/resource/%D1%84%D0%BB%D1%8D%D1%88%D0%B1%D0%B5%D0%BA%D0%B0%D1%85",
      "http://dbpedia.org/resource/флэшбеках"),

    ("arabic, simple non-ASCII characters",
      // should be decoded completely, special: direction change
      "http://pt.dbpedia.org/resource/%D8%AA%D9%85%D8%AA%D9%84%D9%83",
      "http://pt.dbpedia.org/resource/تمتلك"),

    ("chinese, simple non-ASCII characters",
      // should be decoded completely, special: direction change
      "http://example.com/base/%E6%A4%8D%E7%89%A9%2F%E5%90%8D%3D%E3%81%97%E3%81%9D%3B%E4%BD%BF%E7%94%A8%E9%83%A8%3D%E8%91%89",
      "http://example.com/base/植物/名=しそ;使用部=葉"),


    /**
      * & in path, Ren & Stimpy
      */


    ("ren & stimpy: %26 before, %26 after",
      // TODO
      "http://dbpedia.org/resource/The_Ren_%26_Stimpy_Show",
      "http://dbpedia.org/resource/The_Ren_%26_Stimpy_Show"),

    ("ren & stimpy in path2",
      // TODO
      "http://dbpedia.org/resource/The_Ren_&_Stimpy_Show",
      "http://dbpedia.org/resource/The_Ren_%26_Stimpy_Show"),

    ("ren & stimpy in path3",
      "http://dbpedia.org/resource/Ren_%26_Stimpy_\"Adult_Party_Cartoon\"",
      "http://dbpedia.org/resource/Ren_%26_Stimpy_%22Adult_Party_Cartoon%22"),

    ("ren & stimpy in path4",
      // TODO
      "http://dbpedia.org/resource/Ren_%26_Stimpy_%22Adult_Party_Cartoon%22",
      "http://dbpedia.org/resource/Ren_%26_Stimpy_%22Adult_Party_Cartoon%22"),


    ("reserved characters",
      // ?#[|] should stay encoded, rest of reserved chars schould be decoded
      "http://dbpedia.org/resource/%21%23%3F%5B%5D%7D%2A",
      "http://dbpedia.org/resource/!%23%3F%5B%5D%7D*"),

    ("unwise characters and double whitespace",
      // "<>[\]^`{|} should be re-encoded and double whitespace gets replaced with one underscore
      "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C  test",
      "http://dbpedia.org/resource/%22%3C%3E%5C%5E%60%7B%7C_test"),

    ("double + instead of whitespace",
      // should be decoded completely
      "http://dbpedia.org/resource/Jeanne  Deroubaix",
      "http://dbpedia.org/resource/Jeanne_Deroubaix"),


    ("encoding-depth > 1",
      // should be decoded completely
      "http://pt.dbpedia.org/resource/%25C3%2581rea_de_Re…",
      "http://pt.dbpedia.org/resource/Área_de_Re…"),

    ("invalid Escape Sequence: too short",
      // should not throw an error, should just be ignored
      "http://pt.dbpedia.org/resource/foo%3",
      "http://pt.dbpedia.org/resource/foo%3"),

    ("invalid Escape Sequence: not hexadecimal",
      // should not throw an error, should just be ignored
      "http://pt.dbpedia.org/resource/foo%2K",
      "http://pt.dbpedia.org/resource/foo%2K"),


    ("query test",
      // should be decoded completely, special: direction change
      "http://dbpedia-live.openlinksw.com/sparql/?default-graph-uri=http%3A%2F%2Fstatic.dbpedia.org&qtxt=describe+%3Chttp%3A%2F%2Fdbpedia.org%2Fresource%2FAmsterdam%3E&format=text%2Fx-html%2Bul&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on",
      "http://dbpedia-live.openlinksw.com/sparql/?default-graph-uri=http://static.dbpedia.org&qtxt=describe+%3Chttp://dbpedia.org/resource/Amsterdam%3E&format=text/x-html+ul&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on"),

    ("simple test",
      // should be decoded completely, special: direction change
      "http://www.example.org/red%09ros%C3%A9#red",
      "http://www.example.org/red\trosé#red"),

    ("simple testNotSoSimple",
      // should be decoded completely, special: direction change
      "http://example.com/%F0%90%8C%80%F0%90%8C%81%F0%90%8C%82",
      "http://example.com/\uD800\uDF00\uD800\uDF01\uD800\uDF02"),


    ("testx1",
      // should be decoded completely, special: direction change
      "http://iris.test.ing/foo%3fbar",
      "http://iris.test.ing/foo?bar"),

    ("testx3",
      // should be decoded completely, special: direction change
      "http://foo/path;a??e#f#g",
      "http://foo/path;a??e#f#g")


  )
  //val testList = List [(name:String, test:String,result:String)] {("","","")}
  testList.foreach(t => {
    test(t._1) {
      val decoded = UriUtils.uriToIri(t._2)
      val resultIri = IRI.create(t._3).get
      if (log == true) {
        info("TestURI: " + t._2)
        info("Decoded: " + decoded)
        info("Exp.IRI: " + resultIri)
      }
      //test with equals
      assert(decoded.equals(resultIri),
        "test(" + t._1 + ")\n" +
          "TestURI: " + t._2 + "\n" +
          "Decoded: " + decoded + "\n" +
          "Exp.IRI: " + resultIri + "")

      //test with string equals
      assert(decoded.toString.equals(resultIri.toString))
    }


  })

}


/*
  "testx2") {
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


  "testx4") {
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
*/