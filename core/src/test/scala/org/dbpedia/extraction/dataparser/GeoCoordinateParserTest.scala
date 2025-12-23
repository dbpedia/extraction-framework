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
  // === QUADRANT VARIATIONS ===

  // West longitude (negative)
  "GeoCoordinateParser - west longitude" should "return (43.7, -79.42)" in {
    parse("en", "{{coord|43.7|N|79.42|W}}") should equal (Some(43.7, -79.42))
  }

  // South latitude (negative)
  "GeoCoordinateParser - south latitude" should "return (-30.5595, 22.9375)" in {
    parse("en", "{{coord|30.5595|S|22.9375|E}}") should equal (Some(-30.5595, 22.9375))
  }

  // Southwest quadrant (both negative)
  "GeoCoordinateParser - southwest quadrant" should "return (-33.45, -70.66)" in {
    parse("en", "{{coord|33.45|S|70.66|W}}") should equal (Some(-33.45, -70.66))
  }

  // === AFRICA DIRECTION MAPPINGS ===

  // Swahili - testing Swahili directions
  "GeoCoordinateParser - Swahili directions" should "return (-1.2921, 36.8219)" in {
    parse("sw", "{{coord|1.2921|S|36.8219|E}}") should equal (Some(-1.2921, 36.8219))
  }

  // Amharic - testing Ethiopian directions
  "GeoCoordinateParser - Amharic directions" should "return (9.1450, 40.4897)" in {
    parse("am", "{{coord|9.1450|N|40.4897|E}}") should equal (Some(9.1450, 40.4897))
  }

  // Afrikaans - testing South African directions
  "GeoCoordinateParser - Afrikaans directions" should "return (-33.9249, 18.4241)" in {
    parse("af", "{{coord|33.9249|S|18.4241|O}}") should equal (Some(-33.9249, 18.4241))
  }

  // === ADDITIONAL TESTS FOR DBPEDIA EXTRACTION LANGUAGES ===

// English - United States (Washington DC coordinates)
"GeoCoordinateParser - English United States Washington DC" should "return (38.8833, -77.0167)" in {
  parse("en", "{{coord|38.8833|N|77.0167|W}}") should equal (Some(38.8833, -77.0167))
}

// German - Vereinigte Staaten (Geographic center coordinates)
"GeoCoordinateParser - German United States geographic center" should "return (40.0, -100.0)" in {
  parse("de", "{{coord|40.0|N|100.0|W}}") should equal (Some(40.0, -100.0))
}

// French - États-Unis (Geographic center variant)
"GeoCoordinateParser - French United States geographic variant" should "return (40.0, -105.0)" in {
  parse("fr", "{{coord|40.0|N|105.0|W}}") should equal (Some(40.0, -105.0))
}

// German - Französisch-Guayana
"GeoCoordinateParser - German French Guiana variant" should "return (4.0, -53.0)" in {
  parse("de", "{{Coordinate|NS=4//|EW=53///W|type=adm1st|dim=400000|region=FR-973}}") should equal (Some(4.0, -53.0))
}

// German - Mexiko
"GeoCoordinateParser - German Mexico " should "return (23.3166, -102.3666)" in {
  parse("de", "{{Coordinate |NS=23/19//N |EW=102/22//W |type=country |region=MX}}") should equal (Some(23.3166, -102.3666))
}

// German - Tonga
"GeoCoordinateParser - German Tonga" should "return (-20.5877, -174.8102)" in {
  parse("de", "{{Coordinate |NS=20/35/16/S |EW=174/48/37/W |type=country |region=TO}}") should equal (Some(-20.5877, -174.8102))
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
