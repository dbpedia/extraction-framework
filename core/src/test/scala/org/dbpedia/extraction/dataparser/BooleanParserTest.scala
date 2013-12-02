package org.dbpedia.extraction.dataparser

import _root_.org.dbpedia.extraction.sources.WikiPage
import _root_.org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BooleanParserTest extends FlatSpec with ShouldMatchers
{
    "BooleanParser" should "return true" in
    {
        parse("true") should equal (Some(true))
        parse("  true") should equal (Some(true))
        parse("true  ") should equal (Some(true))

        parse("is true") should equal (Some(true))
        parse("true story") should equal (Some(true))
        parse("is a very true story") should equal (Some(true))

        parse("yes") should equal (Some(true))
    }

    it should "return false" in
    {
        parse("false") should equal (Some(false))
        parse("  false") should equal (Some(false))
        parse("false  ") should equal (Some(false))
        parse("is false") should equal (Some(false))
        parse("false story") should equal (Some(false))
        parse("is a very false story") should equal (Some(false))

        parse("no") should equal (Some(false))
    }

    it should "return None" in
    {
        parse("foo") should equal (None)
        parse("The Beatles song 'Yesterday'") should equal (None)
        parse("MONOTONE") should equal (None)
    }

    private val parser = WikiParser.getInstance()

    private def parse(input : String) : Option[Boolean] =
    {
        val page = new WikiPage(WikiTitle.parse("TestPage", Language.English), input)
        
        BooleanParser.parse(parser(page))
    }
}
