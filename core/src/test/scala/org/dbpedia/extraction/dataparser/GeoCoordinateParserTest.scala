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
  // === DISTINCT COORDINATE FORMAT PATTERNS ===

  // Pattern: degrees°minutes'seconds"direction (non-template format)
  "GeoCoordinateParser - DMS with symbols" should "return (51.2, 3.216666666666667)" in {
    parse("fr", "51º12'00\"N 03º13'00\"E") should equal(Some(51.2, 3.216666666666667))
  }

  // Pattern: {{coord|decimal|direction|decimal|direction}} (2 coordinate pairs)
  "GeoCoordinateParser - decimal degrees with cardinal directions" should "return (51.2, 31.2)" in {
    parse("fr", "{{coord|51.2|N|31.2|E}}") should equal(Some(51.2, 31.2))
  }

  // Pattern: {{coord|degrees|minutes|direction|degrees|minutes|direction}} (Wikipedia-compliant)
  "GeoCoordinateParser - degrees and minutes only" should "return (51.2, 3.216666666666667)" in {
    parse("fr", "{{coord|51|12|N|03|13|E}}") should equal(Some(51.2, 3.216666666666667))
  }


  // Pattern: {{coord|degrees|minutes|direction|degrees|minutes|direction}} (4 parameters)
  "GeoCoordinateParser - degrees and minutes only" should "return (52.5, 13.416666666666666)" in {
    parse("en", "{{coord|52|30|N|13|25|E}}") should equal(Some(52.5, 13.416666666666666))
  }

  // Pattern: {{coord|degrees|minutes|seconds|direction|degrees|minutes|seconds|direction}} (6 parameters)
  "GeoCoordinateParser - degrees minutes seconds" should "return (-34.833333333333336, 20.0)" in {
    parse("en", "{{coord|34|50|0|S|20|0|0|E}}") should equal(Some(-34.833333333333336, 20.0))
  }

  // Pattern: {{coord|degrees|minutes|decimal_seconds|direction|degrees|minutes|decimal_seconds|direction}} (6 parameters with decimal seconds)
  "GeoCoordinateParser - DMS with decimal seconds" should "return (-33.92486111111111, 18.424055555555555)" in {
    parse("en", "{{coord|33|55|29.5|S|18|25|26.6|E}}") should equal(Some(-33.92486111111111, 18.424055555555555))
  }

  // Pattern: {{coord|decimal|direction|decimal|direction|additional_params}} (with ignored parameters)
  "GeoCoordinateParser - coordinates with ignored parameters" should "return (51.477, 0.0)" in {
    parse("en", "{{coord|51.477|N|0.0|E|display=title|type:city|region:GB}}") should equal(Some(51.477, 0.0))
  }

  // === QUADRANT VARIATIONS ===

  // West longitude (negative)
  "GeoCoordinateParser - west longitude" should "return (43.7, -79.42)" in {
    parse("en", "{{coord|43.7|N|79.42|W}}") should equal(Some(43.7, -79.42))
  }

  // South latitude (negative)
  "GeoCoordinateParser - south latitude" should "return (-30.5595, 22.9375)" in {
    parse("en", "{{coord|30.5595|S|22.9375|E}}") should equal(Some(-30.5595, 22.9375))
  }

  // Southwest quadrant (both negative)
  "GeoCoordinateParser - southwest quadrant" should "return (-33.45, -70.66)" in {
    parse("en", "{{coord|33.45|S|70.66|W}}") should equal(Some(-33.45, -70.66))
  }

  // === EDGE CASES ===

  // Equator and Prime Meridian
  "GeoCoordinateParser - equator and prime meridian" should "return (0.0, 0.0)" in {
    parse("en", "{{coord|0.0|N|0.0|E}}") should equal(Some(0.0, 0.0))
  }

  // High precision coordinates
  "GeoCoordinateParser - high precision decimal" should "return (40.748817, -73.985428)" in {
    parse("en", "{{coord|40.748817|N|73.985428|W}}") should equal(Some(40.748817, -73.985428))
  }

  // Near pole coordinates
  "GeoCoordinateParser - arctic coordinates" should "return (89.9, -135.0)" in {
    parse("en", "{{coord|89.9|N|135.0|W}}") should equal(Some(89.9, -135.0))
  }

  // === TEMPLATE VARIATIONS ===

  // Capitalized template name
  "GeoCoordinateParser - capitalized Coord template" should "return (51.5074, -0.1278)" in {
    parse("en", "{{Coord|51.5074|N|0.1278|W}}") should equal(Some(51.5074, -0.1278))
  }

  // Alternative coordinate templates
  "GeoCoordinateParser - coor template" should "return (48.8566, 2.3522)" in {
    parse("en", "{{coor|48.8566|N|2.3522|E}}") should equal(Some(48.8566, 2.3522))
  }

  "GeoCoordinateParser - coordonnées template" should "return (45.7640, 4.8357)" in {
    parse("fr", "{{coordonnées|45.7640|N|4.8357|E}}") should equal(Some(45.7640, 4.8357))
  }

  "GeoCoordinateParser - coordinaten template" should "return (52.3702, 4.8952)" in {
    parse("nl", "{{coordinaten|52.3702|N|4.8952|E}}") should equal(Some(52.3702, 4.8952))
  }

  "GeoCoordinateParser - unsupported coordinate template" should "return None" in {
    parse("en", "{{geopoint|40.4165|N|3.7026|W}}") should equal(None)
  }

  // === LANGUAGE-SPECIFIC DIRECTION MAPPINGS ===

  // Arabic - testing Arabic directions
  "GeoCoordinateParser - Arabic directions" should "return (24.7136, 46.6753)" in {
    parse("ar", "{{coord|24.7136|شمال|46.6753|شرق}}") should equal(Some(24.7136, 46.6753))
  }

  // Arabic - testing Arabic south/west
  "GeoCoordinateParser - Arabic south west" should "return (-15.2993, -28.0473)" in {
    parse("ar", "{{coord|15.2993|جنوب|28.0473|غرب}}") should equal(Some(-15.2993, -28.0473))
  }

  // Bulgarian - testing Cyrillic directions
  "GeoCoordinateParser - Bulgarian Cyrillic directions" should "return (42.6977, 23.3219)" in {
    parse("bg", "{{coord|42.6977|С|23.3219|И}}") should equal(Some(42.6977, 23.3219))
  }

  // Czech - testing Czech directions with V for East
  "GeoCoordinateParser - Czech directions" should "return (50.0755, 14.4378)" in {
    parse("cs", "{{coord|50.0755|N|14.4378|V}}") should equal(Some(50.0755, 14.4378))
  }

  // German - testing German O for East
  "GeoCoordinateParser - German directions" should "return (52.5200, 13.4050)" in {
    parse("de", "{{coord|52.5200|N|13.4050|O}}") should equal(Some(52.5200, 13.4050))
  }

  // Spanish - testing Spanish O for West
  "GeoCoordinateParser - Spanish directions" should "return (40.4168, -3.7038)" in {
    parse("es", "{{coord|40.4168|N|3.7038|O}}") should equal(Some(40.4168, -3.7038))
  }

  // French - testing French O for East (opposite of Spanish)
  "GeoCoordinateParser - French O for East" should "return (45.7640, 4.8357)" in {
    parse("fr", "{{coord|45.7640|N|4.8357|O}}") should equal(Some(45.7640, 4.8357))
  }

  // Hindi - testing Hindi Devanagari directions
  "GeoCoordinateParser - Hindi directions" should "return (28.7041, 77.1025)" in {
    parse("hi", "{{coord|28.7041|उ|77.1025|पू}}") should equal(Some(28.7041, 77.1025))
  }

  // Italian - testing Italian O for West
  "GeoCoordinateParser - Italian O for West" should "return (45.4642, -9.1900)" in {
    parse("it", "{{coord|45.4642|N|9.1900|O}}") should equal(Some(45.4642, -9.1900))
  }

  // Japanese - testing Japanese directions
  "GeoCoordinateParser - Japanese directions" should "return (35.6762, 139.6503)" in {
    parse("ja", "{{coord|35.6762|北|139.6503|東}}") should equal(Some(35.6762, 139.6503))
  }

  // Dutch - testing Dutch O for East
  "GeoCoordinateParser - Dutch directions" should "return (52.3702, 4.8952)" in {
    parse("nl", "{{coord|52.3702|N|4.8952|O}}") should equal(Some(52.3702, 4.8952))
  }

  // Polish - testing Polish abbreviations
  "GeoCoordinateParser - Polish abbreviations" should "return (50.0647, 19.9450)" in {
    parse("pl", "{{coord|50.0647|płn|19.9450|wsch}}") should equal(Some(50.0647, 19.9450))
  }

  // Portuguese - testing Portuguese O for West
  "GeoCoordinateParser - Portuguese directions" should "return (-23.5505, -46.6333)" in {
    parse("pt", "{{coord|23.5505|S|46.6333|O}}") should equal(Some(-23.5505, -46.6333))
  }

  // Russian - testing Russian Cyrillic В for East
  "GeoCoordinateParser - Russian Cyrillic directions" should "return (55.7558, 37.6176)" in {
    parse("ru", "{{coord|55.7558|С|37.6176|В}}") should equal(Some(55.7558, 37.6176))
  }

  // Chinese - testing Chinese directions
  "GeoCoordinateParser - Chinese directions" should "return (39.9042, 116.4074)" in {
    parse("zh", "{{coord|39.9042|北|116.4074|东}}") should equal(Some(39.9042, 116.4074))
  }

  // === MALFORMED COORDINATE TESTS ===

  // Missing direction indicators
  "GeoCoordinateParser - missing direction indicators" should "return None" in {
    parse("en", "{{coord|40.7589|73.9851}}") should equal(None)
  }

  // Invalid direction indicators
  "GeoCoordinateParser - invalid direction indicators" should "return None" in {
    parse("en", "{{coord|40.7589|X|73.9851|Y}}") should equal(None)
  }

  // Too many parameters
  "GeoCoordinateParser - too many coordinate parameters" should "return None" in {
    parse("en", "{{coord|40|30|15|10|N|73|58|30|20|W}}") should equal(None)
  }

  // Empty coordinate template
  "GeoCoordinateParser - empty coordinate template" should "return None" in {
    parse("en", "{{coord}}") should equal(None)
  }

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
      case Some(n) => geoCoordinateParser.parse(n).map({ x => (x.value.latitude, x.value.longitude) })
      case None => None
    }
  }
}