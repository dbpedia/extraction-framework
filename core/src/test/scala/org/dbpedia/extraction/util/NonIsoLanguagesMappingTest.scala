package org.dbpedia.extraction.util

import java.util.Locale
import org.junit.runner.RunWith
import org.scalatest.junit._
import org.scalatest._
import Inspectors._

import io.{Codec, Source}

/**
 * Tests if the Map Language.nonIsoWpCodes is complete, so that for each MediaWiki language code
 * that is not also a ISO 639-1 language code, there exists a mapping to a related ISO 639-1 language code. 
 */
@RunWith(classOf[JUnitRunner])
class NonIsoLanguagesMappingTest extends FlatSpec with Matchers
{
        // get all existing language codes for which a Wikipedia exists
        val source = Source.fromURL(Language.wikipediaLanguageUrl)(Codec.UTF8)
        val wikiLanguageCodes = try source.getLines.toList finally source.close

    "Wiki langauge codes" should " be more than 0" in {
        wikiLanguageCodes.size should be > 0
    }
        // get all ISO 639-1 language codes
        val isoLanguageCodes = Locale.getISOLanguages

    "ISO 639-1 codes" should " be more than 0" in {
        wikiLanguageCodes.size should be > 0
    }

        // get all Wikipedia language codes that are not ISO 639-1 language codes
        val wpNonIsoLanguageCodes = wikiLanguageCodes.toSet &~ isoLanguageCodes.toSet

    "Every wiki-code" should " have a mapping in the non-ISO code map" in {
        forAll(wikiLanguageCodes) {
            Language.map.keys should contain(_)
        }
    }

    "Every non-ISO code map entry " should "point to a ISO 639-1 language code" in {
        // if a mapping exists, check if the mapping points to a ISO 639-1 language code
        forAll(wikiLanguageCodes) {x: String =>
            isoLanguageCodes should contain(Language.map(x).isoCode)
        }
    }

}