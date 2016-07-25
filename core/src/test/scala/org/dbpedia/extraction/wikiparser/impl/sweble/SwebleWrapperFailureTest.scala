package org.dbpedia.extraction.wikiparser.impl.sweble

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import org.scalatest.junit.JUnitRunner


/**
  *
  */
@RunWith(classOf[JUnitRunner])
class SwebleWrapperFailureTest extends FlatSpec with Matchers with PrivateMethodTester {

  "SwebleWrapper" should """Fail for fail for ParserFunctionNodes""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
        {{Infobox Test1

                 | data37     = {{#if: temp_string1 | temp_string2 | temp_string3  }}

        }}
               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    var h = 9
    (sweblePageNode) should not be (simplePageNode)

  }
}


object WikiParserWrapper {

  val simpleWikiParser = new SimpleWikiParser()
  val swebleWikiParser = new SwebleWrapper()

}
