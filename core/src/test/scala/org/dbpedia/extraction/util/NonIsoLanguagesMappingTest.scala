package org.dbpedia.extraction.util

import java.util.Locale
import io.{Codec, Source}
import java.net.URL

/**
 * Tests if the Map Language.nonIsoWpCodes is complete, so that for each MediaWiki language code
 * that is not also a ISO 639-1 language code, there exists a mapping to a related ISO 639-1 language code. 
 */
object NonIsoLanguagesMappingTest
{
    //TODO make this a proper Scala Test class
    def main(args : Array[String])
    {
        // get all existing language codes for which a Wikipedia exists
        val source = Source.fromURL("http://noc.wikimedia.org/conf/langlist")(Codec.UTF8)
        val wikiLanguageCodes = try source.getLines.toList finally source.close

        // get all ISO 639-1 language codes
        val isoLanguageCodes = Locale.getISOLanguages

        // get all Wikipedia language codes that are not ISO 639-1 language codes
        val wpNonIsoLanguageCodes = wikiLanguageCodes.toSet &~ isoLanguageCodes.toSet
        
        var errorCount = 0
        for (wpNonIsoCode <- wpNonIsoLanguageCodes) {
            try {
                // check if this Wikipedia language code already has a mapping in the nonIsoWpCodes map
                val language = Language(wpNonIsoCode) 
                // if a mapping exists, check if the mapping points to a ISO 639-1 language code
                if (! isoLanguageCodes.contains(language.isoCode)) {
                    println("* mapping to non-ISO code: '"+wpNonIsoCode+"' -> '"+language.isoCode+"'")
                    errorCount += 1
                }
            }
            catch {
                case _ : IllegalArgumentException => 
                    println("* no mapping for non-ISO code '"+wpNonIsoCode+"'")
                    errorCount += 1
            }
        }
        
        println("\nTest finished. "+errorCount+"/"+wpNonIsoLanguageCodes.size+" failed.")
        if (errorCount == 0) {
            println("Non-iso languages map is complete.")
        }
    }

}