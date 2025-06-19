package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiTitle, WikiParser}
import org.dbpedia.extraction.sources.MemorySource
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeoCoordinateParserTest extends FlatSpec with Matchers
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
   
    // Tests for Amharic
    "GeoCoordinateParser(20º12'00\"N 03º13'00\"E)" should "return (20.2,3.216666666666667))" in
    {
        parse("am", "20º12'00\"N 03º13'00\"E") should equal (Some(20.2,3.216666666666667))
    }
    "GeoCoordinateParser({{coord|10.2|N|13.2|E}}" should "return (10.2,13.2)) for Amharic" in
    {
        parse("am", "{{coord|10.2|N|13.2|E}}") should equal (Some(10.2,13.2))
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

      wikiParser(page) match
      {
        case Some(n) => geoCoordinateParser.parse(n).map({x => (x.value.latitude, x.value.longitude)})
        case None => None
      }
    }
}
