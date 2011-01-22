package org.dbpedia.extraction.dataparser

import org.scalatest.FlatSpec
import org.dbpedia.extraction.ontology.OntologyDatatypes
import org.dbpedia.extraction.util.Language
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}


class DurationParserTest extends FlatSpec with ShouldMatchers
{
    "Duration" should "return 5 seconds for '5 secs'" in
     {
         parse("en", "Time", "5 secs").get.toSeconds should equal (5.0)
     }
    "Duration" should "return 10 minutes for 'context 10 min context'" in
     {
         parse("en", "Time", "context 10 min context").get.toSeconds should equal (600.0)
     }
    "Duration" should "return 14820 seconds for 'context 4 hours, 7 minutes context'" in
     {
         parse("en", "Time", "context 4 hours, 7 minutes context").get.toSeconds should equal (14820.0)
     }
    "Duration" should "return 14730 seconds for ' 4h 5m, 30s '" in
     {
         parse("en", "Time", " 4h 5m, 30s ").get.toSeconds should equal (14730.0)
     }
    "Duration" should "return 857752817 seconds for 'ganze 27 years, 2 months, 5 days, 1h 40min., 17s hat es gedauert '" in
     {
         parse("en", "Time", "ganze 27 years, 2 months, 5 days, 1h 40min., 17s hat es gedauert ").get.toSeconds should be (approximatelyEqualTo(857752817.0))
     }
    "Duration" should "return 188 seconds for 'context 3m 8s context'" in
     {
         parse("en", "Time", "context 3m 8s context").get.toSeconds should equal (188.0)
     }
    "Duration" should "return 2417 seconds for '40min., 17s'" in
     {
         parse("en", "Time", "40min., 17s").get.toSeconds should equal (2417.0)
     }
    "Duration" should "return aprox. 32263698 seconds for '1y 8d 4h 7m 8s'" in
     {
         parse("en", "Time", "1y 8d 4h 7m 8s").get.toSeconds should be (approximatelyEqualTo(32263628.0))
     }
    "Duration" should "return 604997.0 seconds for '27 years, 2 GLUCK, 5 days, 1hrs. 40min., 17s'" in
     {
         parse("en", "Time", "7 days, 5 DOLLARS, 3 min., 8 METERS, 17s").get.toSeconds should equal (604997.0)
     }
    // don't match characters after units
    "Duration" should "return 660 seconds for '11 min '" in
     {
         parse("en", "Time", "11 min ").get.toSeconds should equal (660.0)
     }
    "Duration" should "return 660 seconds for '(11 min) asd'" in
     {
         parse("en", "Time", "(11 min)").get.toSeconds should equal (660.0)
     }
    "Duration" should "return 660 seconds for '11 min,'" in
     {
         parse("en", "Time", "11 min,").get.toSeconds should equal (660.0)
     }
    "Duration" should "return 660 seconds for '11 min/'" in
     {
         parse("en", "Time", "11 min/").get.toSeconds should equal (660.0)
     }
    "Duration" should "return 86400 seconds for '1d/'" in
     {
         parse("en", "Time", "1d/").get.toSeconds should equal (86400.0)
     }
    // Dots, commas, spaces
    "Duration" should "return 435649 seconds for '5.042 234 days'" in
     {
         parse("en", "Time", "5.042 234 days").get.toSeconds should equal (435649.01759999996)
     }
    "Duration" should "return 48124803628 seconds for '577,000.042 d'" in
     {
         parse("en", "Time", "577,000.042 d").get.toSeconds should equal (49852803628.8)
     }
    "Duration" should "return 48124803628 seconds for '577.000,042 Tage'" in
     {
         parse("de", "Time", "577.000,042 Tage").get.toSeconds should equal (49852803628.8)
     }
    "Duration" should "return 9954414708530 seconds for '115,213,133.2 days, 50 secs'" in
     {
         parse("en", "Time", "115,213,133.2 days, 50 secs").get.toSeconds should equal (9954414708530.0)
     }
    // Colon separated
    "Duration" should "return 30 seconds for ':30'" in
     {
         parse("en", "Time", ":30").get.toSeconds should equal (30.0)
     }
    "Duration" should "return 30 seconds for ':30 sec'" in
     {
         parse("en", "Time", ":30 sec").get.toSeconds should equal (30.0)
     }
    "Duration" should "return 90 seconds for '1:30'" in
     {
         parse("en", "Time", "1:30 min").get.toSeconds should equal (90.0)
     }
    "Duration" should "return 90 minutes for '1:30 h'" in
     {
         parse("en", "Time", "1:30 h").get.toSeconds should equal (5400.0)
     }
    "Duration" should "return 15030 seconds for '4:10:30'" in
     {
         parse("en", "Time", "4:10:30").get.toSeconds should equal (15030.0)
     }
    "Duration" should "return 20430 seconds for 'bla 5:40:30 h long'" in
     {
         parse("en", "Time", "bla 5:40:30 h long").get.toSeconds should equal (20430.0)
     }
    "Duration" should "return 20430 seconds for 'bla 5:40:30 super'" in
     {
         parse("en", "Time", "bla 5:40:30 super").get.toSeconds should equal (20430.0)
     }
    // These tests should fail:
    "Duration" should "return None for 'bla 5:40:30 seconds'" in
     {
         parse("en", "Time", "bla 5:40:30 seconds") should equal (None)
     }
    "Duration" should "return None for '2002-present'" in
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


    private val datatypes =  OntologyDatatypes.load.map(dt => (dt.name, dt)).toMap

    private def parse(language : String, datatypeName : String, input : String) : Option[Duration] =
    {

        val locale = Language.fromWikiCode(language).get.locale

        val inputDatatype = datatypes(datatypeName)

        Duration.parse(input, inputDatatype, locale)
    }
}
