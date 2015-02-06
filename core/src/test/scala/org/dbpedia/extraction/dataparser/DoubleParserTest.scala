package org.dbpedia.extraction.dataparser

import junit.framework.TestCase
import junit.framework.Assert._
import org.dbpedia.extraction.wikiparser.{Node, TextNode}
import org.dbpedia.extraction.util.Language

class DoubleParserTest extends TestCase
{
    def testParse() : Unit =
    {
        testParse("bg", "42.697556", Some(42.697556))
        testParse("bg", "42,697556", Some(42.697556))
        testParse("en", "123.45", Some(123.45))
        testParse("en", "1,234.5", Some(1234.5))
        testParse("de", "123,45", Some(123.45))
        testParse("de", "1.234,5", Some(1234.5))
        testParse("en", ".12345", Some(0.12345))
        testParse("de", ",12345", Some(0.12345))
        testParse("nl", "1,234", Some(1.234))
        testParse("nl", ",12345", Some(0.12345))
    }

    private def testParse( lang : String, value : String, expect : Option[Double] ) : Unit =
    {
      testParse(lang, new TextNode(value, 1), expect)
      // testParse(lang, new ExternalLinkNode(URI.create("http:foo"), List(new TextNode(value, 1)), 1), expect)
      // testParse(lang, new ExternalLinkNode(URI.create("http:foo"), List(new TextNode("dummy", 1), new TextNode(value, 1)), 1), expect)
    }

    private def testParse( lang : String, node : Node, expect : Option[Double] ) : Unit =
    {
      def parser = new DoubleParser(new {def language : Language = Language(lang)})
      assertEquals(expect, parser.parse(node))
    }

}