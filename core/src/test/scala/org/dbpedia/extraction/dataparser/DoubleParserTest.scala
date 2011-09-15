package org.dbpedia.extraction.dataparser

//import junit.framework.TestCase
//import junit.framework.Assert._
//import org.dbpedia.extraction.wikiparser._
//import java.net.URI
//
//class DoubleParserTest extends TestCase
//{
//    def testParse() : Unit =
//    {
//        testParse("en", "123.45", Some(123.45))
//        testParse("en", "1,234.5", Some(1234.5))
//        testParse("de", "123,45", Some(123.45))
//        testParse("de", "1.234,5", Some(1234.5))
//    }
//
//    private def testParse( lang : String, value : String, expect : Option[Double] ) : Unit =
//    {
//      testParse(lang, new TextNode(value, 1), expect)
//      testParse(lang, new ExternalLinkNode(URI.create("http://foo"), List(new TextNode(value, 1)), 1), expect)
//      testParse(lang, new ExternalLinkNode(URI.create("http://foo"), List(new TextNode("dummy", 1), new TextNode(value, 1)), 1), expect)
//    }
//
//    private def testParse( lang : String, node : Node, expect : Option[Double] ) : Unit =
//    {
//      //TODO assertEquals(expect, new {def language : Language = Language.fromWikiCode(lang).get} ).parse(node))
//    }
//
//}