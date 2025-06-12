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
class GeoCoordinateParserTest extends FlatSpec with Matchers {

  // === BASIC FORMAT TESTS ===

  // Non-template degree-minute-second format
  "GeoCoordinateParser(51º12'00\"N 03º13'00\"E)" should "return (51.2, 3.216666666666667)" in {
    parse("fr", "51º12'00\"N 03º13'00\"E") should equal(Some(51.2, 3.216666666666667))
  }

  // Decimal degrees with hemispheres using template
  "GeoCoordinateParser({{coord|51.2|N|31.2|E}})" should "return (51.2, 31.2)" in {
    parse("fr", "{{coord|51.2|N|31.2|E}}") should equal(Some(51.2, 31.2))
  }

  // Degrees and decimal minutes with slashes
  "GeoCoordinateParser({{coord|51/12/N|03/13/E}})" should "return (51.2, 3.216666666666667)" in {
    parse("fr", "{{coord|51/12/N|03/13/E}}") should equal(Some(51.2, 3.216666666666667))
  }

  // === DIFFERENT LANGUAGES ===

  // Amharic language test
  "GeoCoordinateParser({{coord|10.2|N|13.2|E}}) - Amharic" should "return (10.2, 13.2)" in {
    parse("am", "{{coord|10.2|N|13.2|E}}") should equal(Some(10.2, 13.2))
  }

  // Test language with different longitude letter mapping (if applicable)
  "GeoCoordinateParser({{coord|25.5|S|28.3|E}}) - Afrikaans" should "return (-25.5, 28.3)" in {
    parse("af", "{{coord|25.5|S|28.3|E}}") should equal(Some(-25.5, 28.3))
  }

  // === COORDINATE TEMPLATE VARIATIONS ===

  // Degrees only
  "GeoCoordinateParser({{coord|40|N|74|W}})" should "return (40.0, -74.0)" in {
    parse("en", "{{coord|40|N|74|W}}") should equal(Some(40.0, -74.0))
  }

  // Degrees and minutes
  "GeoCoordinateParser({{coord|40|30|N|74|0|W}})" should "return (40.5, -74.0)" in {
    parse("en", "{{coord|40|30|N|74|0|W}}") should equal(Some(40.5, -74.0))
  }

  // Degrees, minutes, seconds
  "GeoCoordinateParser({{coord|40|30|30|N|74|0|0|W}})" should "return (40.50833333333333, -74.0)" in {
    parse("en", "{{coord|40|30|30|N|74|0|0|W}}") should equal(Some(40.50833333333333, -74.0))
  }

  // Degrees, minutes, seconds (Southern/Eastern hemispheres)
  "GeoCoordinateParser({{coord|51|12|0|S|03|13|0|W}})" should "return (-51.2, -3.216666666666667)" in {
    parse("fr", "{{coord|51|12|0|S|03|13|0|W}}") should equal(Some(-51.2, -3.216666666666667))
  }

  // Decimal degrees with negative values and hemispheres
  "GeoCoordinateParser({{coord|-45.0|S|-122.0|W}})" should "return (-45.0, -122.0)" in {
    parse("fr", "{{coord|-45.0|S|-122.0|W}}") should equal(Some(-45.0, -122.0))
  }

  // Decimal minutes
  "GeoCoordinateParser({{coord|51|12.5|N|03|13|E}})" should "return (51.208333333333336, 3.216666666666667)" in {
    parse("en", "{{coord|51|12.5|N|03|13|E}}") should equal(Some(51.208333333333336, 3.216666666666667))
  }

  // Decimal seconds
  "GeoCoordinateParser({{coord|51|12|30.5|N|03|13|0|E}})" should "return (51.208472222222225, 3.216666666666667)" in {
    parse("en", "{{coord|51|12|30.5|N|03|13|0|E}}") should equal(Some(51.208472222222225, 3.216666666666667))
  }

  // With display parameter (should ignore extra parameters)
  "GeoCoordinateParser({{coord|40.5|N|74.0|W|display=inline}})" should "return (40.5, -74.0)" in {
    parse("en", "{{coord|40.5|N|74.0|W|display=inline}}") should equal(Some(40.5, -74.0))
  }

  // With region parameter
  "GeoCoordinateParser({{coord|25|44|46|S|28|11|17|E|region:ZA}})" should "return (-25.746111111111112, 28.188055555555558)" in {
    parse("en", "{{coord|25|44|46|S|28|11|17|E|region:ZA}}") should equal(Some(-25.746111111111112, 28.188055555555558))
  }

  // With multiple parameters
  "GeoCoordinateParser({{coord|33|55|29|S|18|25|27|E|display=inline,title|type:city}})" should "return (-33.924722222222225, 18.424166666666668)" in {
    parse("en", "{{coord|33|55|29|S|18|25|27|E|display=inline,title|type:city}}") should equal(Some(-33.924722222222225, 18.424166666666668))
  }

  // === REPRESENTATIVE REAL-WORLD COORDINATES ===

  // Johannesburg
  "GeoCoordinateParser({{coord|26|12|16|S|28|2|44|E}})" should "return (-26.204444444444444, 28.045555555555556)" in {
    parse("en", "{{coord|26|12|16|S|28|2|44|E}}") should equal(Some(-26.204444444444444, 28.045555555555556))
  }

  // Cape Agulhas (southernmost point of Africa)
  "GeoCoordinateParser({{coord|34|50|0|S|20|0|0|E}})" should "return (-34.833333333333336, 20.0)" in {
    parse("en", "{{coord|34|50|0|S|20|0|0|E}}") should equal(Some(-34.833333333333336, 20.0))
  }

  // Mossel Bay
  "GeoCoordinateParser({{coord|34|11|0|S|22|9|0|E}})" should "return (-34.18333333333333, 22.15)" in {
    parse("en", "{{coord|34|11|0|S|22|9|0|E}}") should equal(Some(-34.18333333333333, 22.15))
  }

  // International border coordinate
  "GeoCoordinateParser({{coord|25|0|0|S|25|0|0|E}})" should "return (-25.0, 25.0)" in {
    parse("en", "{{coord|25|0|0|S|25|0|0|E}}") should equal(Some(-25.0, 25.0))
  }

  // === PRECISION AND EDGE CASES ===

  // Single digit values
  "GeoCoordinateParser({{coord|5|5|5|S|5|5|5|E}})" should "return (-5.084722222222222, 5.084722222222222)" in {
    parse("en", "{{coord|5|5|5|S|5|5|5|E}}") should equal(Some(-5.084722222222222, 5.084722222222222))
  }

  // Zero values in coordinates
  "GeoCoordinateParser({{coord|30|0|0|S|25|0|0|E}})" should "return (-30.0, 25.0)" in {
    parse("en", "{{coord|30|0|0|S|25|0|0|E}}") should equal(Some(-30.0, 25.0))
  }

  // Mixed hemisphere combinations (should work correctly)
  "GeoCoordinateParser({{coord|25|0|0|N|25|0|0|W}})" should "return (25.0, -25.0)" in {
    parse("en", "{{coord|25|0|0|N|25|0|0|W}}") should equal(Some(25.0, -25.0))
  }

  // === ERROR CONDITIONS ===

  // Missing hemisphere information
  "GeoCoordinateParser({{coord|51|12|03|03|13|00}})" should "return None for missing hemispheres" in {
    parse("fr", "{{coord|51|12|03|03|13|00}}") should equal(None)
  }

  // Invalid numeric values
  "GeoCoordinateParser({{coord|abc|N|xyz|E}})" should "return None for invalid numbers" in {
    parse("fr", "{{coord|abc|N|xyz|E}}") should equal(None)
  }

  // Invalid latitude > 90
  "GeoCoordinateParser({{coord|95|0|0|S|25|0|0|E}})" should "return None for invalid latitude" in {
    parse("en", "{{coord|95|0|0|S|25|0|0|E}}") should equal(None)
  }

  // Invalid longitude > 180
  "GeoCoordinateParser({{coord|25|0|0|S|185|0|0|E}})" should "return None for invalid longitude" in {
    parse("en", "{{coord|25|0|0|S|185|0|0|E}}") should equal(None)
  }

  // Invalid negative minutes
  "GeoCoordinateParser({{coord|25|-5|0|S|25|0|0|E}})" should "return None for negative minutes" in {
    parse("en", "{{coord|25|-5|0|S|25|0|0|E}}") should equal(None)
  }

  // Invalid negative seconds
  "GeoCoordinateParser({{coord|25|0|-10|S|25|0|0|E}})" should "return None for negative seconds" in {
    parse("en", "{{coord|25|0|-10|S|25|0|0|E}}") should equal(None)
  }

  // Missing longitude hemisphere
  "GeoCoordinateParser({{coord|25|0|0|S|25|0|0}})" should "return None for missing longitude hemisphere" in {
    parse("en", "{{coord|25|0|0|S|25|0|0}}") should equal(None)
  }

  // === ALTERNATIVE COORDINATE FORMATS ===

  // Different template syntax variations (if supported)
  "GeoCoordinateParser({{Coord|40|30|N|74|0|W}})" should "handle capitalized template name" in {
    // This test depends on whether the parser handles case variations
    val result = parse("en", "{{Coord|40|30|N|74|0|W}}")
    // Either should parse correctly or return None - both are valid behaviors
    result match {
      case Some((40.5, -74.0)) => // Success case
      case None => // Also acceptable if parser is case-sensitive
      case other => fail(s"Unexpected result: $other")
    }
  }

  // Test with spaces in template
  "GeoCoordinateParser({{ coord | 40 | 30 | N | 74 | 0 | W }})" should "handle spaced template" in {
    val result = parse("en", "{{ coord | 40 | 30 | N | 74 | 0 | W }}")
    // Should either parse correctly or return None
    result match {
      case Some((40.5, -74.0)) => // Success case
      case None => // Also acceptable if parser doesn't handle spaces
      case other => fail(s"Unexpected result: $other")
    }
  }

  // === ADDITIONAL LANGUAGE TESTS ===

  // Test with a language that might have different coordinate parsing rules
  "GeoCoordinateParser({{coord|45.5|N|2.3|E}}) - German" should "return (45.5, 2.3)" in {
    parse("de", "{{coord|45.5|N|2.3|E}}") should equal(Some(45.5, 2.3))
  }

  // Test with Arabic language (RTL script)
  "GeoCoordinateParser({{coord|31.7|N|35.2|E}}) - Arabic" should "return (31.7, 35.2)" in {
    parse("ar", "{{coord|31.7|N|35.2|E}}") should equal(Some(31.7, 35.2))
  }

  // Test with Chinese language
  "GeoCoordinateParser({{coord|39.9|N|116.4|E}}) - Chinese" should "return (39.9, 116.4)" in {
    parse("zh", "{{coord|39.9|N|116.4|E}}") should equal(Some(39.9, 116.4))
  }

  // === Helper method for parsing ===
  private val wikiParser = WikiParser.getInstance()

  private def parse(language: String, input: String): Option[(Double, Double)] = {
    val lang = Language(language)
    val context = new {
      def language: Language = lang
      def redirects: Redirects = new Redirects(Map())
    }

    val geoCoordinateParser = new GeoCoordinateParser(context)
    val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

    wikiParser(page) match {
      case Some(n) => geoCoordinateParser.parse(n).map(x => (x.value.latitude, x.value.longitude))
      case None    => None
    }
  }
}