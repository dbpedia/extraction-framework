package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.sources.WikiPage
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.mappings.Redirects
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FlagTemplateParserTest extends FlatSpec with ShouldMatchers
{
    "FlagTemplateParser" should "return United States@en" in
    {
        parse("en", "{{flag|United States}}") should equal (Some("United States"))
    }
    "FlagTemplateParser" should "return France@en" in
    {
        parse("en", "{{flag|FRA}}") should equal (Some("France"))
    }
    "FlagTemplateParser" should "return Germany@en" in
    {
        parse("en", "{{flag|DEU|empire}}") should equal (Some("Germany"))
    }
    "FlagTemplateParser" should "return Italy@en" in
    {
        parse("en", "{{flagcountry|ITA}}") should equal (Some("Italy"))
    }
    "FlagTemplateParser" should "return Angleterre@fr" in
    {
        parse("fr", "{{drapeau2|Angleterre|domaine=gentilé|genre=féminin}}") should equal (Some("Angleterre"))
    }
    "FlagTemplateParser" should "return South Korea@en" in
    {
        parse("en", "{{KOR}}") should equal (Some("South Korea"))
    }
    "FlagTemplateParser" should "return Germany@en (CIO)" in
    {
        parse("en", "{{GER}}") should equal (Some("Germany"))
    }
    "FlagTemplateParser" should "return Francja@pl" in
    {
        parse("pl", "{{FRA}}") should equal (Some("Francja"))
    }
    "FlagTemplateParser" should "return Nederland@nl" in
    {
        parse("nl", "{{NL}}") should equal (Some("Nederland"))
    }

    private val parser = WikiParser.getInstance()

    private def parse(language : String, input : String) : Option[String] =
    {
        val lang = Language(language)
        val red = new Redirects(Map())
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)
        val context = new
        {
            def language : Language = lang
        }
        
        val flagParser = new FlagTemplateParser(context)
    
        parser(page) match {
          case Some(n) => Some(flagParser.parse(n.children.head).getOrElse(return None).decoded)
          case None => None
        }

    }
}
