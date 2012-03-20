package org.dbpedia.extraction.dataparser

import org.scalatest.FlatSpec
import org.dbpedia.extraction.ontology.OntologyDatatypes
import org.dbpedia.extraction.util.Language
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}

class DurationParserTest extends FlatSpec with ShouldMatchers
{
    "DurationParser" should "return 5 seconds for '5 secs'" in
     {
         parse("en", "Time", "5 secs").get should equal (5.0)
     }
    "DurationParser" should "return 10 minutes for 'context 10 min context'" in
     {
         parse("en", "Time", "context 10 min context").get should equal (600.0)
     }
    "DurationParser" should "return 14820 seconds for 'context 4 hours, 7 minutes context'" in
     {
         parse("en", "Time", "context 4 hours, 7 minutes context").get should equal (14820.0)
     }
    "DurationParser" should "return 14730 seconds for ' 4h 5m, 30s '" in
     {
         parse("en", "Time", " 4h 5m, 30s ").get should equal (14730.0)
     }
    "DurationParser" should "return 857752817 seconds for 'ganze 27 years, 2 months, 5 days, 1h 40min., 17s hat es gedauert '" in
     {
         parse("en", "Time", "ganze 27 years, 2 months, 5 days, 1h 40min., 17s hat es gedauert ").get should be (approximatelyEqualTo(857752817.0))
     }
    "DurationParser" should "return 188 seconds for 'context 3m 8s context'" in
     {
         parse("en", "Time", "context 3m 8s context").get should equal (188.0)
     }
    "DurationParser" should "return 2417 seconds for '40min., 17s'" in
     {
         parse("en", "Time", "40min., 17s").get should equal (2417.0)
     }
    "DurationParser" should "return aprox. 32263698 seconds for '1y 8d 4h 7m 8s'" in
     {
         parse("en", "Time", "1y 8d 4h 7m 8s").get should be (approximatelyEqualTo(32263628.0))
     }
    "DurationParser" should "return 604997.0 seconds for '27 years, 2 GLUCK, 5 days, 1hrs. 40min., 17s'" in
     {
         parse("en", "Time", "7 days, 5 DOLLARS, 3 min., 8 METERS, 17s").get should equal (604997.0)
     }
    // don't match characters after units
    "DurationParser" should "return 660 seconds for '11 min '" in
     {
         parse("en", "Time", "11 min ").get should equal (660.0)
     }
    "DurationParser" should "return 660 seconds for '(11 min) asd'" in
     {
         parse("en", "Time", "(11 min)").get should equal (660.0)
     }
    "DurationParser" should "return 660 seconds for '11 min,'" in
     {
         parse("en", "Time", "11 min,").get should equal (660.0)
     }
    "DurationParser" should "return 660 seconds for '11 min/'" in
     {
         parse("en", "Time", "11 min/").get should equal (660.0)
     }
    "DurationParser" should "return 86400 seconds for '1d/'" in
     {
         parse("en", "Time", "1d/").get should equal (86400.0)
     }
    // Dots, commas, spaces
    "DurationParser" should "return 435649 seconds for '5.042 234 days'" in
     {
         parse("en", "Time", "5.042 234 days").get should equal (435649.01759999996)
     }
    "DurationParser" should "return 48124803628 seconds for '577,000.042 d'" in
     {
         parse("en", "Time", "577,000.042 d").get should equal (49852803628.8)
     }
    "DurationParser" should "return 48124803628 seconds for '577.000,042 Tage'" in
     {
         parse("de", "Time", "577.000,042 Tage").get should equal (49852803628.8)
     }
    "DurationParser" should "return 9954414708530 seconds for '115,213,133.2 days, 50 secs'" in
     {
         parse("en", "Time", "115,213,133.2 days, 50 secs").get should equal (9954414708530.0)
     }
    // Colon separated
    "DurationParser" should "return 30 seconds for ':30'" in
     {
         parse("en", "Time", ":30").get should equal (30.0)
     }
    "DurationParser" should "return 30 seconds for ':30 sec'" in
     {
         parse("en", "Time", ":30 sec").get should equal (30.0)
     }
    "DurationParser" should "return 90 seconds for '1:30'" in
     {
         parse("en", "Time", "1:30 min").get should equal (90.0)
     }
    "DurationParser" should "return 90 minutes for '1:30 h'" in
     {
         parse("en", "Time", "1:30 h").get should equal (5400.0)
     }
    "DurationParser" should "return 15030 seconds for '4:10:30'" in
     {
         parse("en", "Time", "4:10:30").get should equal (15030.0)
     }
    "DurationParser" should "return 20430 seconds for 'bla 5:40:30 h long'" in
     {
         parse("en", "Time", "bla 5:40:30 h long").get should equal (20430.0)
     }
    "DurationParser" should "return 20430 seconds for 'bla 5:40:30 super'" in
     {
         parse("en", "Time", "bla 5:40:30 super").get should equal (20430.0)
     }
    // These tests should fail:
    "DurationParser" should "return None for 'bla 5:40:30 seconds'" in
     {
         parse("en", "Time", "bla 5:40:30 seconds") should equal (None)
     }
    "DurationParser" should "return None for '2002-present'" in
     {
         parse("en", "Time", "2002-present") should equal (None)
     }

     /**
     * Matcher to test if 2 values are approximately equal.
     */
    case class approximatelyEqualTo(r : Double) extends BeMatcher[Double]
    {
        val epsilon = 0.001

        def apply(l: Double) =
            MatchResult(
                compare(l, r),
                l + " is not approximately equal to " + r,
                l + " is approximately equal to " + r
            )

        private def compare(l : Double, r : Double) : Boolean =
        {
            scala.math.abs(l - r) < epsilon
        }
    }


    private val datatypes =  OntologyDatatypes.load().map(dt => (dt.name, dt)).toMap

    private def parse(lang : String, datatypeName : String, input : String) : Option[Double] =
    {
        val context = new
        {
            def language : Language = Language.forCode(lang)
        }

        val durationParser = new DurationParser(context)

        val inputDatatype = datatypes(datatypeName)

        durationParser.parseToSeconds(input, inputDatatype)
    }
}
