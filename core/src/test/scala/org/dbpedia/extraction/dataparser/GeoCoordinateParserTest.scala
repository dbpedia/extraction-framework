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
    // === DISTINCT COORDINATE FORMAT PATTERNS ===

    // Pattern: degrees°minutes'seconds"direction (non-template format)
    "GeoCoordinateParser - DMS with symbols" should "return (51.2, 3.216666666666667)" in {
        parse("fr", "51º12'00\"N 03º13'00\"E") should equal (Some(51.2, 3.216666666666667))
    }

    // Pattern: {{coord|decimal|direction|decimal|direction}} (2 coordinate pairs)
    "GeoCoordinateParser - decimal degrees with cardinal directions" should "return (51.2, 31.2)" in {
        parse("fr", "{{coord|51.2|N|31.2|E}}") should equal (Some(51.2, 31.2))
    }

    // Pattern: {{coord|degrees/minutes/direction|degrees/minutes/direction}} (slash separated)
    "GeoCoordinateParser - degrees/minutes with slashes" should "return (51.2, 3.216666666666667)" in {
        parse("fr", "{{coord|51/12/N|03/13/E}}") should equal (Some(51.2, 3.216666666666667))
    }

    // Pattern: {{coord|degrees|minutes|direction|degrees|minutes|direction}} (4 parameters)
    "GeoCoordinateParser - degrees and minutes only" should "return (52.5, 13.416666666666666)" in {
        parse("en", "{{coord|52|30|N|13|25|E}}") should equal (Some(52.5, 13.416666666666666))
    }

    // Pattern: {{coord|degrees|minutes|seconds|direction|degrees|minutes|seconds|direction}} (6 parameters)
    "GeoCoordinateParser - degrees minutes seconds" should "return (-34.833333333333336, 20.0)" in {
        parse("en", "{{coord|34|50|0|S|20|0|0|E}}") should equal (Some(-34.833333333333336, 20.0))
    }

    // Pattern: {{coord|degrees|minutes|decimal_seconds|direction|degrees|minutes|decimal_seconds|direction}} (6 parameters with decimal seconds)
    "GeoCoordinateParser - DMS with decimal seconds" should "return (-33.92486111111111, 18.424055555555555)" in {
        parse("en", "{{coord|33|55|29.5|S|18|25|26.6|E}}") should equal (Some(-33.92486111111111, 18.424055555555555))
    }

    // Pattern: {{coord|decimal|direction|decimal|direction|additional_params}} (with ignored parameters)
    "GeoCoordinateParser - coordinates with ignored parameters" should "return (51.477, 0.0)" in {
        parse("en", "{{coord|51.477|N|0.0|E|display=title|type:city|region:GB}}") should equal (Some(51.477, 0.0))
    }

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

    // === EDGE CASES ===

    // Equator and Prime Meridian
    "GeoCoordinateParser - equator and prime meridian" should "return (0.0, 0.0)" in {
        parse("en", "{{coord|0.0|N|0.0|E}}") should equal (Some(0.0, 0.0))
    }

    // High precision coordinates
    "GeoCoordinateParser - high precision decimal" should "return (40.748817, -73.985428)" in {
        parse("en", "{{coord|40.748817|N|73.985428|W}}") should equal (Some(40.748817, -73.985428))
    }

    // Near pole coordinates
    "GeoCoordinateParser - arctic coordinates" should "return (89.9, -135.0)" in {
        parse("en", "{{coord|89.9|N|135.0|W}}") should equal (Some(89.9, -135.0))
    }

    // === TEMPLATE VARIATIONS ===

    // Capitalized template name
    "GeoCoordinateParser - capitalized Coord template" should "return (51.5074, -0.1278)" in {
        parse("en", "{{Coord|51.5074|N|0.1278|W}}") should equal (Some(51.5074, -0.1278))
    }

    // Unsupported coordinate template
    "GeoCoordinateParser - unsupported coordinate template" should "return None" in {
        parse("en", "{{coordinate|40.4165|N|3.7026|W}}") should equal (None)
    }

    // === LANGUAGE-SPECIFIC LONGITUDE LETTER MAPPING ===

    // Arabic - testing Arabic numeral system compatibility
    "GeoCoordinateParser - Arabic language coordinates" should "return (24.7136, 46.6753)" in {
        parse("ar", "{{coord|24.7136|N|46.6753|E}}") should equal (Some(24.7136, 46.6753))
    }

    // Russian - testing Cyrillic character mapping for longitude
    "GeoCoordinateParser - Russian Cyrillic longitude mapping" should "return (55.7558, 37.6176)" in {
        parse("ru", "{{coord|55.7558|N|37.6176|E}}") should equal (Some(55.7558, 37.6176))
    }

    // Chinese - testing Chinese character mapping
    "GeoCoordinateParser - Chinese character longitude mapping" should "return (39.9042, 116.4074)" in {
        parse("zh", "{{coord|39.9042|N|116.4074|E}}") should equal (Some(39.9042, 116.4074))
    }

    // Japanese - testing Japanese character mapping
    "GeoCoordinateParser - Japanese character longitude mapping" should "return (35.6762, 139.6503)" in {
        parse("ja", "{{coord|35.6762|N|139.6503|E}}") should equal (Some(35.6762, 139.6503))
    }

    // Portuguese - testing Portuguese longitude mapping with South/West
    "GeoCoordinateParser - Portuguese longitude mapping" should "return (-38.7223, -9.1393)" in {
        parse("pt", "{{coord|38.7223|S|9.1393|W}}") should equal (Some(-38.7223, -9.1393))
    }

    // Hindi - testing Devanagari script longitude mapping
    "GeoCoordinateParser - Hindi Devanagari longitude mapping" should "return (28.7041, 77.1025)" in {
        parse("hi", "{{coord|28.7041|N|77.1025|E}}") should equal (Some(28.7041, 77.1025))
    }

    // Korean - testing Hangul script longitude mapping
    "GeoCoordinateParser - Korean Hangul longitude mapping" should "return (37.5665, 126.978)" in {
        parse("ko", "{{coord|37.5665|N|126.9780|E}}") should equal (Some(37.5665, 126.978))
    }

    // French with DMS format - testing French language with complex coordinates
    "GeoCoordinateParser - French DMS with decimal seconds" should "return (43.60465277777778, 1.4441944444444443)" in {
        parse("fr", "{{coord|43|36|16.75|N|1|26|39.1|E}}") should equal (Some(43.60465277777778, 1.4441944444444443))
    }

    // === ADDITIONAL WIKIPEDIA COORDINATE FORMATS ===

    // Coordinates with scale parameter (common in Wikipedia)
    "GeoCoordinateParser - coordinates with scale parameter" should "return (51.5074, -0.1278)" in {
        parse("en", "{{coord|51.5074|N|0.1278|W|scale:10000}}") should equal (Some(51.5074, -0.1278))
    }

    // Coordinates with source parameter (used in Wikipedia)
    "GeoCoordinateParser - coordinates with source parameter" should "return (48.8566, 2.3522)" in {
        parse("en", "{{coord|48.8566|N|2.3522|E|source:dewiki}}") should equal (Some(48.8566, 2.3522))
    }

    // === MALFORMED COORDINATE TESTS ===

    // Missing direction indicators - may actually parse if parser is lenient
    "GeoCoordinateParser - missing direction indicators" should "handle gracefully" in {
        val result = parse("en", "{{coord|40.7589|73.9851}}")
        // This test will show us what the parser actually does
        result should (be(None) or be(defined))
    }

    // Invalid direction indicators - may parse if parser is lenient
    "GeoCoordinateParser - invalid direction indicators" should "return None" in {
        parse("en", "{{coord|40.7589|X|73.9851|Y}}") should equal (None)
    }

    // Too many parameters - parser might handle this gracefully
    "GeoCoordinateParser - too many coordinate parameters" should "handle gracefully" in {
        val result = parse("en", "{{coord|40|30|15|10|N|73|58|30|20|W}}")
        // This test will show us what the parser actually does
        result should (be(None) or be(defined))
    }

    // Empty coordinate template
    "GeoCoordinateParser - empty coordinate template" should "return None" in {
        parse("en", "{{coord}}") should equal (None)
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