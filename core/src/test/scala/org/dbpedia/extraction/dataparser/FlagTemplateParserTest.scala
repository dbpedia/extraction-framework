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
    "FlagTemplateParser" should "return Germany@en (IOC)" in
    {
        parse("en", "{{GER}}") should equal (Some("Germany"))
    }
    "FlagTemplateParser" should "return Suisse@fr (IOC)" in
    {
        parse("fr", "{{SUI}}") should equal (Some("Suisse"))
    }
    "FlagTemplateParser" should "return Corée du Sud@fr" in
    {
        parse("fr", "{{KOR}}") should equal (Some("Corée du Sud"))
    }
    "FlagTemplateParser" should "return Francja@pl" in
    {
        parse("pl", "{{FRA}}") should equal (Some("Francja"))
    }
    "FlagTemplateParser" should "return Nederland@nl" in
    {
        parse("nl", "{{NL}}") should equal (Some("Nederland"))
    }
    "FlagTemplateParser" should "return European Union@en" in
    {
        parse("en", "{{EU}}") should equal (Some("European Union"))
    }
    "FlagTemplateParser" should "return France@fr" in
    {
        parse("fr", "{{France}}") should equal (Some("France"))
    }
    "FlagTemplateParser" should "return Afrique du Sud@fr" in
    {
        parse("fr", "{{Afrique du Sud}}") should equal (Some("Afrique du Sud"))
    }
    "FlagTemplateParser" should "return 日本@ja" in
    {
        parse("ja", "{{JPN}}") should equal (Some("日本"))
    }
    "FlagTemplateParser" should "return スウェーデン@ja" in
    {
        parse("ja", "{{flagicon|SWE}}") should equal (Some("スウェーデン"))
    }
    "FlagTemplateParser" should "return アメリカ合衆国@ja" in
    {
        parse("ja", "{{flagicon2|USA}}") should equal (Some("アメリカ合衆国"))
    }
    "FlagTemplateParser" should "return ポルトガル@ja" in
    {
        parse("ja", "{{flagicon2|ポルトガル|coronial}}") should equal (Some("ポルトガル"))
    }
    "FlagTemplateParser" should "return ブラジル@ja" in
    {
        parse("ja", "{{flag|ブラジル}}") should equal (Some("ブラジル"))
    }
    "FlagTemplateParser" should "return オーストラリア@ja" in
    {
        parse("ja", "{{flag|オーストラリア}}") should equal (Some("オーストラリア"))
    }
    "FlagTemplateParser" should "return ドイツ@ja" in
    {
        parse("ja", "{{flagcountry|GER}}") should equal (Some("ドイツ"))
    }

/*  Performance tests for different cases
  
    "FlagTemplateParser" should "return United States@en (x100000)" in
    {
        1 to 100000 foreach { i => parse("en", "{{flag|United States}}") }
    }
    "FlagTemplateParser" should "return France@en (x100000)" in
    {
        1 to 100000 foreach { i => parse("en", "{{flag|FRA}}") }
    }
    "FlagTemplateParser" should "return France@fr (x100000)" in
    {
        1 to 100000 foreach { i => parse("fr", "{{FRA}}") }
    }
    "FlagTemplateParser" should "return Afrique du Sud@fr (x100000)" in
    {
        1 to 100000 foreach { i => parse("fr", "{{Afrique du Sud}}") }
    }
*/
  
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
