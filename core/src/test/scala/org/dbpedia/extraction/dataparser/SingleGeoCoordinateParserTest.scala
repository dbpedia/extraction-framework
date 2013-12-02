package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser.{WikiTitle, WikiParser}
import org.dbpedia.extraction.sources.{WikiPage,MemorySource}
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SingleGeoCoordinateParserTest extends FlatSpec with ShouldMatchers
{

    "SingleGeoCoordinateParser(51/12/N)" should "return 51.2" in
    {
        parse("fr", "51/12/N") should equal (Some(51.2))
    }
    "SingleGeoCoordinateParser(51/12/S)" should "return -51.2" in
    {
        parse("fr", "51/12/S") should equal (Some(-51.2))
    }
    "SingleGeoCoordinateParser(03/13/O)" should "return -3.216666666666667 for French" in
    {
    	parse("fr", "03/13/O") should equal (Some(-3.216666666666667))
    }
    "SingleGeoCoordinateParser(03/13/O)" should "return 3.216666666666667 for German" in
    {
    	parse("de", "03/13/O") should equal (Some(3.216666666666667))
    }
    

    private val wikiParser = WikiParser.getInstance()

    private def parse(language : String, input : String) : Option[Double] =
    {
        val lang = Language(language)
        val context = new
        {
            def language : Language = lang
            def redirects : Redirects = new Redirects(Map())
        }
        val singleGeoCoordinateParser = new SingleGeoCoordinateParser(context)
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

        singleGeoCoordinateParser.parse(wikiParser(page)).map(_.toDouble)
    }
}
