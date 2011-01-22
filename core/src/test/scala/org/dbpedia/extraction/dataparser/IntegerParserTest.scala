package org.dbpedia.extraction.dataparser

import org.scalatest.FlatSpec
import org.dbpedia.extraction.ontology.OntologyDatatypes
import org.dbpedia.extraction.util.Language
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}
import org.dbpedia.extraction.wikiparser.TextNode
import org.dbpedia.extraction.mappings.ExtractionContext


class IntegerParserTest extends FlatSpec with ShouldMatchers
{
    "IntegerParser" should "return 8 for '8.0'@en" in
     {
         parse("en", "8.0") should equal (Some(8))
     }
    "IntegerParser" should "return 8 for '8,0'@de" in
     {
         parse("de", "8,0") should equal (Some(8))
     }
    "IntegerParser" should "return 12500000 for '12.5 mio'@en" in
     {
         parse("en", "12.5 mio") should equal (Some(12500000))
     }
    "IntegerParser" should "return 12500000 for '12,5 mio kg'@de" in
     {
         parse("de", "12,5 mio kg") should equal (Some(12500000))
     }
    "IntegerParser" should "return 40000000 for '40,000,000 (estimated)'@en" in
     {
         parse("en", "40,000,000 (estimated)") should equal (Some(40000000))
     }
    "IntegerParser" should "return 40000000 for '40.000.000'@de" in
     {
         parse("de", "40.000.000") should equal (Some(40000000))
     }
    "IntegerParser" should "return 40000 for '40,000.000 (estimated)'@en" in
     {
         parse("en", "40,000.000 (estimated)") should equal (Some(40000))
     }
    "IntegerParser" should "return 40000 for '40.000,000'@de" in
     {
         parse("de", "40.000,000") should equal (Some(40000))
     }


    /**
     * Matcher to test if 2 values are approximately equal.
     *
     * NOT NEEDED AT THE MOMENT!
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

    private val datatypes = OntologyDatatypes.load.map(dt => (dt.name, dt)).toMap

    private def parse( lang : String, input : String, strict : Boolean = false, datatypeName : String = "xsd:integer" ) : Option[Int] =
    {
        val context = new ExtractionContext(null, Language.fromWikiCode(lang).get, null, null, null, null)
        val textNode = new TextNode(input, 1)

        val inputDatatype = datatypes(datatypeName)
        val validRange : (Int => Boolean) = inputDatatype.name match
        {
            case "xsd:integer" => (i => true)
            case "xsd:positiveInteger"    => (i => i > 0)
            case "xsd:nonNegativeInteger" => (i => i >=0)
            case "xsd:nonPositiveInteger" => (i => i <=0)
            case "xsd:negativeInteger"    => (i => i < 0)
        }

        val parser = new IntegerParser(context, strict, validRange)
        parser.parse(textNode)
    }

}
