package org.dbpedia.extraction.mappings.rml

import be.ugent.mmlab.rml.model.RMLMapping
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class RMLParserTest extends FlatSpec with Matchers
{

    "RMLParser" should "return RMLMapping" in
    {
        parse() shouldBe a [RMLMapping]
    }

    "RMLParser" should "parse 2 triples map" in
    {
        parse().getTriplesMaps.size() should equal(2)
    }



    def parse(): RMLMapping =
    {
        val pathToDocument = "src/test/resources/org/dbpedia/extraction/mappings/rml/test.rml"
        val rmlMapping = RMLParser.parseFromFile(pathToDocument)
        return rmlMapping

    }
}

