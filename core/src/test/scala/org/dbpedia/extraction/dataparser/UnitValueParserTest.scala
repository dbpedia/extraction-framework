package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{WikiPage,MemorySource}
import org.dbpedia.extraction.util.Language
import org.scalatest.FlatSpec
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}
import scala.math._
import org.dbpedia.extraction.ontology.{Ontology, OntologyDatatypes}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UnitValueParserTest extends FlatSpec with ShouldMatchers
{
   // Length - Positive Tests - Input is valid
    "UnitValueParser" should "return Length(10 m)" in
    {
        parse("en", "Length", "10m") should be (approximatelyEqualTo(Some(10.0)))
    }

    it should "return Length(500 m)" in {
      parse("en", "Length", ".5 km") should be (approximatelyEqualTo(Some(500.0)))
      parse("de", "Length", ",5 km") should be (approximatelyEqualTo(Some(500.0)))
    }

    "UnitValueParser" should "return Length(10 metres)" in
    {
        parse("en", "Length", "10metres") should equal (Some(10.0))
    }
    "UnitValueParser" should "return Length(1 metre)" in
    {
        parse("en", "Length", "1 metre") should equal (Some(1.0))
    }
    "UnitValueParser" should "return Length({{convert|1610|mm|in|1|abbr=on}})" in
    {
        parse("en", "Length", "{{convert|1610|mm|in|1|abbr=on}}") should equal (Some(1.61))
    }

    /**
    // FIXME: Not supported
    "UnitValueParser" should "return Length(210 × 297&nbsp;mm)" in
    {
        parse("en", "Length", "210 × 297&nbsp;mm") should equal (Some(0.297))
    }
    */

    /**
    // FIXME: These templates are not supported! Do they make any sense?!?
    "UnitValueParser" should "return Length({{Infobox mountain | elevation_m = 2181 }})" in
    {
        parse("en", "Length", "{{Infobox mountain | elevation_m = 2181 }}") should equal (Some(2181))
    }
    "UnitValueParser" should "return Length({{Infobox mountain | prominence_m = 471 }})" in
    {
        parse("en", "Length", "{{Infobox mountain | prominence_m = 471 }}") should equal (Some(471))
    }
    "UnitValueParser" should "return Length({{Geobox|Range|highest_elevation=4810.9 }})" in
    {
        parse("en", "Length", "{{Geobox|Range|highest_elevation=4810.9 }}") should equal (Some(4810.9))
    }
    "UnitValueParser" should "return Length({{Infobox road | length_mi = 2451 }})" in
    {
        parse("en", "Length", "{{Infobox road | length_mi = 2451 }}") should equal (Some(3944502.14))
    }
    */

    "UnitValueParser" should "return Length({{convert|112|mm|in|abbr=on}})" in
    {
        parse("en", "Length", "{{convert|112|mm|in|abbr=on}}") should equal (Some(0.112))
    }
    "UnitValueParser" should "return Length({{convert|112|in|mm|abbr=on}})" in
    {
        parse("en", "Length", "{{convert|112|in|mm|abbr=on}}") should be (approximatelyEqualTo(Some(2.8448)))
    }
    "UnitValueParser" should "return Length(6 ft 6 in)" in
    {
        parse("en", "Length", "6 ft 6 in") should be (approximatelyEqualTo(Some(1.9812)))
    }
    "UnitValueParser" should "return Length(6 feet 6 inch)" in
    {
        parse("en", "Length", "6 feet 6 inch") should be (approximatelyEqualTo(Some(1.9812)))
    }
    "UnitValueParser" should "return Length(6' 6\")" in
    {
        parse("en", "Length", "6' 6''") should be (approximatelyEqualTo(Some(1.9812)))
    }
    "UnitValueParser" should "return Length(6')" in
    {
        parse("en", "Length", "6'") should be (approximatelyEqualTo(Some(1.8288)))
    }
    "UnitValueParser" should "return Length(The 7 foot 1 inch Chamberlain )" in
    {
        parse("en", "Length", "The 7 foot 1 inch Chamberlain ") should be  (approximatelyEqualTo(Some(2.1589)))
    }
    "UnitValueParser" should "return Length(10 in)" in
    {
        parse("en", "Length", "10 in") should be  (approximatelyEqualTo(Some(0.254)))
    }
    "UnitValueParser" should "return Length(15ft 10in)" in
    {
        parse("en", "Length", "15ft 10in") should be  (approximatelyEqualTo(Some(4.826)))
    }
    "UnitValueParser" should "return Length({{height|ft=6|in=6}}dgjhgj34)" in
    {
        parse("en", "Length", "{{height|ft=6|in=6}}dgjhgj34") should be  (approximatelyEqualTo(Some(1.9812)))
    }
    "UnitValueParser" should "return Length({{convert|45|mi}} [[Boxing the compass|N]])" in
    {
        parse("en", "Length", "{{convert|45|mi}} [[Boxing the compass|N]]") should be  (approximatelyEqualTo(Some(72420.48)))
    }
    "UnitValueParser" should "return Length(12mm (13in))" in
    {
        parse("en", "Length", "12mm (13in)") should equal (Some(0.012))
    }

    /**
    // FIXME: Not supported.
    "UnitValueParser" should "return Length(The '''22 [[nanometre|nanometer]]''' node )" in
    {
        parse("en", "Length", "The '''22 [[nanometre|nanometer]]''' node ") should equal (Some(0.000000022))
    }
    */
    "UnitValueParser" should "return Length(longwards of 0.7 µm)" in
    {
        parse("en", "Length", "longwards of 0.7 µm") should equal (Some(0.0000007))
    }

    // https://en.wikipedia.org/wiki/Template:Height
    /**
     * {{height|m=1.77|precision=0}}	1.77 m (5 ft 10 in)
     * {{height|m=1.77|frac=16}}	1.77 m (5 ft 9 11⁄16 in)
     * {{height|ft=6}}	6 ft 0 in (1.83 m)
     * {{height|ft=6|in=1}}	6 ft 1 in (1.85 m)
     * {{height|ft=6|in=1|precision=1}}	6 ft 1 in (1.9 m)
     * {{height|ft=5|in=7+1/2}}	5 ft 7 1⁄2 in (1.71 m)
     * {{height|ft=5|in=7+1/2|precision=1}}	5 ft 7 1⁄2 in (1.7 m)
     * {{height|in=10}}	0 ft 10 in (0.25 m)
     * {{height|m=1.72|precision=0}}	1.72 m (5 ft 8 in)
     * {{height|ft=6|in=2|abbr=mos}}	6 feet 2 inches (1.88 m)
     * {{height|ft=6|in=2}}	6 ft 2 in (1.88 m)
     * {{height|ft=5|in=7+1/2|precision=3}}	5 ft 7 1⁄2 in (1.715 m)
     */

    // {{height|m=1.77|precision=0}}
    // {{height|m=1.77|frac=16}}
    it should "return 1.77 m with {{height|m=1.77|precision=0}}" in {
      parse("en", "Length", "{{height|m=1.77|precision=0}}") should equal (Some(1.77))
    }

    it should "return 1.77 m with {{height|m=1.77|frac=16}}" in {
      parse("en", "Length", "{{height|m=1.77|frac=16}}") should equal (Some(1.77))
    }

    // {{height|ft=6}}
    it should "return 1.8288 m with {{height|ft=6}}" in {
      parse("en", "Length", "{{height|ft=6}}") should equal (Some(1.8288))
    }

    // {{height|ft=6|in=1}}
    // {{height|ft=6|in=1|precision=1}}
    it should "return 1.85 m with {{height|ft=6|in=1}}" in {
      parse("en", "Length", "{{height|ft=6|in=1}}") should be (Some(1.8542))
    }

    it should "return 1.85 m with {{height|ft=6|in=1|precision=1}}" in {
      parse("en", "Length", "{{height|ft=6|in=1|precision=1}}") should equal (Some(1.8542))
    }

    // {{height|in=10}}
    it should "return 0.254 m with {{height|in=10}}" in {
      parse("en", "Length", "{{height|in=10}}") should equal (Some(0.254))
    }

    // {{height|m=1.72|precision=0}}
    it should "return 1.72 m with {{height|m=1.72|precision=0}}" in {
      parse("en", "Length", "{{height|m=1.72|precision=0}}") should equal (Some(1.72))
    }

    // {{height|ft=6|in=2|abbr=mos}}
    // {{height|ft=6|in=2}}
    it should "return 1.88 m with {{height|ft=6|in=2|abbr=mos}}" in {
      parse("en", "Length", "{{height|ft=6|in=2|abbr=mos}}") should equal (Some(1.8796))
    }

    it should "return 1.88 m with {{height|ft=6|in=2}}" in {
      parse("en", "Length", "{{height|ft=6|in=2}}") should equal (Some(1.8796))
    }

    //Area - Positive Tests - Input is valid
    // Removed!
    // https://en.wikipedia.org/wiki/Template:Pop_density_mi2_to_km2
    /** "UnitValueParser" should "return Area({{Pop density mi2 to km2|355|precision=0|abbr=yes}})" in
    {
        parse("en", "Area", "{{Pop density mi2 to km2|355|precision=0|abbr=yes}}") should be (approximatelyEqualTo(Some(919445779.0)))
    }
    */
    "UnitValueParser" should "return Area(10 mm²)" in
    {
        parse("en", "Area", "10 mm²") should equal (Some(0.00001))
    }
    "UnitValueParser" should "return Area(10 cm²)" in
    {
        parse("en", "Area", "10 cm²") should equal (Some(0.001))
    }
    "UnitValueParser" should "return Area(1,200,000 ft²)" in
    {
        parse("en", "Area", "1,200,000 ft²") should be (approximatelyEqualTo(Some(111483.648)))
    }
    "UnitValueParser" should "return Area(21.30 km²)" in
    {
        parse("en", "Area", "21.30 km²") should equal (Some(21300000))
        parse("en", "Area", "21.30 km\u00B2") should equal (Some(21300000))
        parse("en", "Area", "21.30 Km\u00B2") should equal (Some(21300000))
    }
    "UnitValueParser" should "return Area(0.5 km²)" in
    {
      parse("en", "Area", ".5 km²") should equal (Some(500000))
      parse("de", "Area", ",5 km²") should equal (Some(500000))
    }
    "UnitValueParser" should "return Area(21.30 m²)" in
    {
        parse("en", "Area", "21.30 m²") should equal (Some(21.30))
    }
    "UnitValueParser" should "return Area(21.30 mi²)" in
    {
        parse("en", "Area", "21.30 mi²") should be (approximatelyEqualTo(Some(5.5166746743E7)))
    }
    "UnitValueParser" should "return Area(21.30 ha)" in
    {
        parse("en", "Area", "21.30 ha") should equal (Some(213000))
    }
    "UnitValueParser" should "return Area(21.30 Sq ft)" in
    {
        parse("en", "Area", "21.30 Sq ft") should be (approximatelyEqualTo(Some(1.9788347520000003)))
    }
    "UnitValueParser" should "return Area(21.30 Square yard)" in
    {
        parse("en", "Area", "21.30 Square yard") should be (approximatelyEqualTo(Some(17.809512768)))
    }
    "UnitValueParser" should "return Area(344.50 acres (1.39 km²))" in
    {
        parse("en", "Area", "344.50 acres (1.39 km²)") should be (approximatelyEqualTo(Some(1394142.04)))
    }
    "UnitValueParser" should "return Area({{km2 to mi2 | 77 | abbr=yes}})" in
    {
        parse("en", "Area", "{{km2 to mi2 | 77 | abbr=yes}}") should be (approximatelyEqualTo(Some(77000000.0)))
    }

    //Volume - Positive Tests - Input is valid
   "UnitValueParser" should "return Volume(10 km³)" in
    {
        parse("en", "Volume", "10 km³") should equal (Some(10000000000.0))
    }
    "UnitValueParser" should "return Volume(0.5 km³)" in
    {
      parse("en", "Volume", ".5 km³") should equal (Some(500000000.0))
      parse("de", "Volume", ",5 km³") should equal (Some(500000000.0))
    }
    "UnitValueParser" should "return Volume(10 m³)" in
    {
        parse("en", "Volume", "10 m³") should equal (Some(10))
    }
    "UnitValueParser" should "return Volume(10 cm3)" in
    {
        parse("en", "Volume", "10 cm3") should equal (Some(0.00001))
    }
    "UnitValueParser" should "return Volume(10 cubic decimetre)" in
    {
        parse("en", "Volume", "10 cubic decimetre") should equal (Some(0.01))
    }
    // TODO: fluid ounces are not supported
    /*
    "UnitValueParser" should "return Volume(12 U.S. fl oz)" in
    {
        parse("en", "Volume", "12 U.S. fl oz") should be (approximatelyEqualTo(Some(0.0000355)))
    }
    "UnitValueParser" should "return Volume(12.5 imp fl oz)" in
    {
        parse("en", "Volume", "12.5 imp fl oz") should be (approximatelyEqualTo(Some(0.0000355)))
    }
    */
    // TODO: plurals are not supported
    /*
    "UnitValueParser" should "return Volume(is 42 US gallons)" in
    {
        parse("en", "Volume", " is 42 US gallons") should be (approximatelyEqualTo(Some(158.9873)))
    }
    */
    "UnitValueParser" should "return Volume({{convert|612000000|USgal|m3|abbr=on}})" in
    {
        parse("en", "Volume", "{{convert|612000000|USgal|m3|abbr=on}}") should be (approximatelyEqualTo(Some(2316672.0)))
    }

    //Time - Positive Tests - Input is valid
    "UnitValueParser" should "return Time(5 Days)" in
    {
        parse("en", "Time", "5 Days") should equal (Some(432000))
    }
    // TODO: Valid? This is very unusual
    /**
    "UnitValueParser" should "return Time(2h 03:59)" in
    {
        parse("en", "Time", "2h 03:59") should equal (Some(7439))
    }
    */
    // TODO: Support ' and "
    /**
    "UnitValueParser" should "return Time(under 2h 10'30\")" in
    {
        parse("en", "Time", "under 2h 10'30\"") should equal (Some(7830))
    }
    */
    "UnitValueParser" should "return Time(in 2 hours 15 minutes and 25 seconds)" in
    {
        parse("en", "Time", "in 2 hours 15 minutes and 25 seconds") should equal (Some(8125))
    }
    "UnitValueParser" should "return Time(A latency of 10 milliseconds )" in
    {
        parse("en", "Time", "A latency of 10 milliseconds ") should equal (Some(0.01))
    }
    "UnitValueParser" should "return Time(2.5 d)" in
    {
        parse("en", "Time", "2.5 d") should equal (Some(216000))
    }
    "UnitValueParser" should "return Time(0.5 hours)" in
    {
        parse("en", "Time", ".5 hours") should equal (Some(1800))
        parse("de", "Time", ",5 hours") should equal (Some(1800))
    }
    "UnitValueParser" should "return Time(-2 min)" in
    {
        parse("en", "Time", "-2 min") should equal (Some(-120))
    }
    "UnitValueParser" should "return Time(asdf:sdag 1:42:05 dsfg)" in
    {
        parse("en", "Time", "asdf:sdag 1:42:05 dsfg") should equal (Some(6125))
    }
    "UnitValueParser" should "return Time(2:35 h)" in
    {
        parse("en", "Time", "2:35 h") should equal (Some(9300))
    }
    "UnitValueParser" should "return Time(2:35 min)" in
    {
        parse("en", "Time", "2:35 min") should equal (Some(155))
    }

    /**
     * Tests for https://en.wikipedia.org/wiki/Template:Duration
     *
     * Named parameters:
     * {{Duration|h=1|m=22|s=34}} renders as: 1:22:34
     * {{Duration|m=74|s=32}} renders as: 74:32
     * {{Duration|m=2|s=34}} renders as: 2:34
     * {{Duration|h=1|s=34}} renders as: 1:00:34
     * {{Duration|h=1}} renders as: 1:00:00
     * {{Duration|h=0|m=22|s=34}} renders as: 0:22:34
     * {{Duration|h=0|m=0|s=34}} renders as: 0:00:34
     * {{Duration|h=1|m=22|s=34.5678}} renders as: 1:22:34.5678
     * {{Duration|h=1|m=22|s=3}} renders as: 1:22:03
     * {{Duration|h=1|m=2|s=34}} renders as: 1:02:34
     * {{Duration|h=1|m=2|s=3}} renders as: 1:02:03
     * {{Duration|h=1|m=2}} renders as: 1:02:00
     * {{Duration|s=34}} renders as: 0:34
     * {{Duration|m=0|s=34}} renders as: 0:34
     * {{Duration|h=1|m=0|s=34}} renders as: 1:00:34
     * {{Duration|h=1|m=22|s=0}} renders as: 1:22:00
     *
     * Numbered parameters:
     * {{Duration|1|22|34}} renders as: 1:22:34
     * {{Duration||74|32}} renders as: 74:32
     * {{Duration|1|2|34}} renders as: 1:02:34
     * {{Duration|1|2|3}} renders as: 1:02:03
     * {{Duration|0|22|34}} renders as: 0:22:34
     * {{Duration|0|0|34}} renders as: 0:00:34
     * {{Duration||0|34}} renders as: 0:34
     * {{Duration|||34}} renders as: 0:34
     * {{Duration|1|22|34.5678}} renders as: 1:22:34.5678
     * {{Duration||1|22}} renders as: 1:22
     * {{Duration|1|22}} renders as: 1:22:00
     * {{Duration|1|22|0}} renders as: 1:22:00
     * {{Duration|0|0|0}} renders as:
     * {{Duration|||}} renders as:
     *
     */

    // {{Duration|h=1|m=22|s=34}}
    // {{Duration|1|22|34}}
    it should "return 1 * 3600.0 + 22 * 60.0 + 34 seconds" in {
      val expected = Some(1 * 3600.0 + 22 * 60.0 + 34)
      parse("en", "Time", "{{Duration|h=1|m=22|s=34}}") should equal (expected)
      parse("en", "Time", "{{Duration|1|22|34}}") should equal (expected)
    }

    // {{Duration|m=74|s=32}}
    // {{Duration||74|32}}
    it should "return 74 * 60.0 + 32 seconds" in {
      val expected = Some(74 * 60.0 + 32)
      parse("en", "Time", "{{Duration|m=74|s=32}}") should equal (expected)
      parse("en", "Time", "{{Duration||74|32}}") should equal (expected)
    }

    // {{Duration|m=2|s=34}}
    it should "return 2 * 60 + 34 seconds" in {
      parse("en", "Time", "{{Duration|m=2|s=34}}") should equal (Some(2 * 60.0 + 34))
    }

    // {{Duration|h=1|s=34}}
    // {{Duration|h=1|m=0|s=34}}
    it should "return 3634.0 seconds" in {
      val expected = Some(3634.0)
      parse("en", "Time", "{{Duration|h=1|s=34}}") should equal (expected)
      parse("en", "Time", "{{Duration|h=1|m=0|s=34}}") should equal (expected)
    }

    // {{Duration|h=1}}
    it should "return 1 * 3600.0 seconds" in {
      parse("en", "Time", "{{Duration|h=1}}") should equal (Some(1 * 3600.0))
    }

    // {{Duration|h=0|m=22|s=34}}
    // {{Duration|0|22|34}}
    it should "return 22 * 60.0 + 34 seconds" in {
      parse("en", "Time", "{{Duration|h=0|m=22|s=34}}") should equal (Some(22 * 60.0 + 34))
      parse("en", "Time", "{{Duration|0|22|34}}") should equal (Some(22 * 60.0 + 34))
    }

    // {{Duration|h=1|m=22|s=34.5678}}
    // {{Duration|1|22|34.5678}}
    it should "return 1 * 3600.0 + 22 * 60.0 + 34.5678 seconds" in {
      parse("en", "Time", "{{Duration|h=1|m=22|s=34.5678}}") should equal (Some(1 * 3600.0 + 22 * 60.0 + 34.5678))
      parse("en", "Time", "{{Duration|1|22|34.5678}}") should equal (Some(1 * 3600.0 + 22 * 60.0 + 34.5678))
    }

    // {{Duration|h=1|m=22|s=3}}
    it should "return 1 * 3600.0 + 22 * 60.0 + 3 seconds" in {
      parse("en", "Time", "{{Duration|h=1|m=22|s=3}}") should equal (Some(1 * 3600.0 + 22 * 60.0 + 3))
    }

    // {{Duration|h=1|m=2|s=34}}
    // {{Duration|1|2|34}}
    it should "return 1 * 3600.0 + 2 * 60.0 + 34 seconds" in {
      val expected = Some(1 * 3600.0 + 2 * 60.0 + 34)
      parse("en", "Time", "{{Duration|h=1|m=2|s=34}}") should equal (expected)
      parse("en", "Time", "{{Duration|1|2|34}}") should equal (expected)
    }

    // {{Duration|h=1|m=2|s=3}}
    // {{Duration|1|2|3}}
    it should "return 1 * 3600.0 + 2 * 60.0 + 3 seconds" in {
      val expected = Some(1 * 3600.0 + 2 * 60.0 + 3)
      parse("en", "Time", "{{Duration|h=1|m=2|s=3}}") should equal (expected)
      parse("en", "Time", "{{Duration|1|2|3}}") should equal (expected)
    }

    // {{Duration|h=1|m=2}}
    it should "return 1 * 3600.0 + 2 * 60.0 seconds" in {
      parse("en", "Time", "{{Duration|h=1|m=2}}") should equal (Some(1 * 3600.0 + 2 * 60.0))
    }

    // {{Duration|s=34}}
    // {{Duration|m=0|s=34}}
    // {{Duration|h=0|m=0|s=34}}
    // {{Duration|0|0|34}}
    // {{Duration||0|34}}
    // {{Duration|||34}}
    it should "return 34 seconds" in {
      parse("en", "Time", "{{Duration|s=34}}") should equal (Some(34))
      parse("en", "Time", "{{Duration|m=0|s=34}}") should equal (Some(34))
      parse("en", "Time", "{{Duration|h=0|m=0|s=34}}") should equal (Some(34))
      parse("en", "Time", "{{Duration|0|0|34}}") should equal (Some(34))
      parse("en", "Time", "{{Duration||0|34}}") should equal (Some(34))
      parse("en", "Time", "{{Duration|||34}}") should equal (Some(34))
    }

    // {{Duration|h=1|m=22|s=0}}
    // {{Duration|1|22}}
    // {{Duration|1|22|0}}
    it should "return 1 * 3600.0 + 22 * 60.0 seconds" in {
      parse("en", "Time", "{{Duration|h=1|m=22|s=0}}") should equal (Some(3600.0 + 22 * 60.0))
      parse("en", "Time", "{{Duration|1|22}}") should equal (Some(3600.0 + 22 * 60.0))
      parse("en", "Time", "{{Duration|1|22|0}}") should equal (Some(3600.0 + 22 * 60.0))
    }

    // {{Duration||1|22}}
    it should "return 1 * 60.0 + 22 seconds" in {
      parse("en", "Time", "{{Duration||1|22}}") should equal (Some(1 * 60.0 + 22))
    }

    // {{Duration|0|0|0}}
    // {{Duration|||}}
    it should "return 0 seconds" in {
      parse("en", "Time", "{{Duration|0|0|0}}") should equal (Some(0))
      parse("en", "Time", "{{Duration|||}}") should equal (Some(0))
    }

    /*
      "UnitValueParser" should "return Time(2:35 min)" in
      {
          parse("en", "Time", "2:35 min") should equal (Some(155))
      }
    */

    "UnitValueParser" should "return Mass(12,5 kg)" in
    {
        parse("en", "Mass", "12.5 kg") should equal (Some(12500))
        parse("de", "Mass", "12,5 kg") should equal (Some(12500))
    }
    "UnitValueParser" should "return Mass({{convert|3.21|kg|lb|0}})" in
    {
        parse("en", "Mass", "{{convert|3.21|kg|lb|0}}") should equal (Some(3210))
    }
    "UnitValueParser" should "return Mass({{convert|3.21|lb|kg|0}})" in
    {
        parse("en", "Mass", "{{convert|3.21|lb|kg|0}}") should be (approximatelyEqualTo(Some(1456.031)))
    }
    // TODO: Would be cool to support fractions
    /**
    "UnitValueParser" should "return Mass(approximately 7 ½ lbs.(3.2&nbsp;kg))" in
    {
        parse("en", "Mass", "approximately 7 ½ lbs.(3.2&nbsp;kg)") should equal (Some(3200))
    }
    */
    
    // metre - Positive Tests - Input is valid
    "UnitValueParser" should "return metre(10)" in
    {
        parse("en", "metre", "10") should be (approximatelyEqualTo(Some(10.0)))
    }

    // au - Positive Tests - Input is valid
    "UnitValueParser" should "return metre(149597870700)" in
    {
      parse("en", "astronomicalUnit", "1") should be (Some(149597870700.0))
    }

    "UnitValueParser" should "return metre(14959787070)" in
    {
      parse("en", "astronomicalUnit", "0.1") should be (Some(14959787070.0))
      parse("en", "astronomicalUnit", ".1") should be (Some(14959787070.0))
      parse("de", "astronomicalUnit", ",1") should be (Some(14959787070.0))
    }

    "UnitValueParser" should "return metre(1.1892681609948355E11)" in
    {
      // https://github.com/dbpedia/extraction-framework/issues/71
      parse("en", "astronomicalUnit", ".7949766633909954") should be (Some(1.1892681609948355E11))
    }

    /**
     * Matcher to test if 2 values are approximately equal.
     */
    case class approximatelyEqualTo(r : Option[Double]) extends BeMatcher[Option[Double]]
    {
        val epsilon = 0.1

        def apply(l: Option[Double]) =
            MatchResult(
                compare(l, r),
                l + " is not approximately equal to " + r,
                l + " is approximately equal to " + r
            )

        private def compare(l : Option[Double], r : Option[Double]) : Boolean =
        {
            (l, r) match
            {
                case (None, None) => true
                case (Some(_), None) => false
                case (None, Some(_)) => false
                case (Some(vl), Some(vr)) => compare(vl, vr)
            }
        }

        private def compare(l : Double, r : Double) : Boolean =
        {
            abs(l - r) < epsilon
        }
    }

    private val wikiParser = WikiParser.getInstance()
    private val datatypes =  OntologyDatatypes.load().map(dt => (dt.name, dt)).toMap

    private def parse(language : String, datatypeName : String, input : String) : Option[Double] =
    {
        val lang = Language(language)
        val red = new Redirects(Map())

        val context = new
        {
            // def ontology : Ontology = throw new Exception("please test without requiring the ontology")
            // Let's fake an ontology with datatypes loaded from OntologyDatatypes - for Time tests
            def ontology : Ontology = new Ontology(Map(), Map(), datatypes , Map(), Map(), Map())
            def language : Language = lang
            def redirects : Redirects = red
        }

        val datatype = datatypes(datatypeName)
        val unitValueParser = new UnitValueParser(context, datatype, false)
        val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

        unitValueParser.parse(wikiParser(page)).map{case (value, dt) => dt.toStandardUnit(value)}
    }
}