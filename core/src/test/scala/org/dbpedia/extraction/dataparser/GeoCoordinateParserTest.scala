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
class GeoCoordinateParserTest extends FlatSpec with ShouldMatchers
{

    "GeoCoordinateParser(51º12'00\"N 03º13'00\"E)" should "return (51.2,3.216666666666667))" in
    {
        parse("fr", "51º12'00\"N 03º13'00\"E") should equal (Some(51.2,3.216666666666667))
    }
    "GeoCoordinateParser({{coord|51.2|N|31.2|E}}" should "return (51.2,31.2)) for French" in
    {
        parse("fr", "{{coord|51.2|N|31.2|E}}") should equal (Some(51.2,31.2))
    }
    "GeoCoordinateParser({{coord|51/12/N|03/13/E}})" should "return (51.2,3.216666666666667))" in
    {
        parse("fr", "{{coord|51/12/N|03/13/E}}") should equal (Some(51.2,3.216666666666667))
    }
    

    private val wikiParser = WikiParser.getInstance()

    private def parse(language : String, input : String) : Option[(Double, Double)] =
    {
        val lang = Language(language)
        val context = new
        {
            def language : Language = lang
            def redirects : Redirects = new Redirects(Map())
        }
        val geoCoordinateParser = new GeoCoordinateParser(context)
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

        geoCoordinateParser.parse(wikiParser(page)).map({x => (x.latitude, x.longitude)})
    }
}
