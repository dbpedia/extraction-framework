package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.util.Language
import scala.util.matching.Regex

/**
 * Utility functions used by the data parsers.
 */
private object ParserUtils
{
    // TODO: many others. Even better: use java.util.Locale settings.
    private val decimalPointLanguages = Set("en", "ja", "zh")
    
    // FIXME: scales are language-dependent - e.g. a billion is 10^12 in German.
    // See http://en.wikipedia.org/wiki/Long_and_short_scales
    private val scales = Map( "thousand" -> 3, "million" -> 6, "mio" -> 6, "billion" -> 9,
                              "mrd" -> 9, "trillion" -> 12, "quadrillion" -> 15 )
                              
    private def pattern( thousands : String, decimal : String ) : (String, Regex) =
    {
        // TODO: use "\s+" instead of "\s?" between number and scale?
        // TODO: in some Asian languages, digits are not separated by thousands but by ten thousands or so...
        (thousands, ("""(?i)([\D]*)([0-9]+(?:\""" + thousands + """[0-9]{3})*)(\""" + decimal + """[0-9]+)?\s?\[?\[?(""" + scales.keySet.mkString("|") + """)\]?\]?(.*)""").r)
    }
    
    private val decimalPointPattern = pattern(",", ".")

    private val decimalCommaPattern = pattern(".", ",")

    /**
     * Converts large numbers like '100.5 million' to '100500000'
     */
    def convertLargeNumbers(input : String, language : Language) : String =
    {
        val (thousands, regex) = if (decimalPointLanguages.contains(language.wikiCode)) decimalPointPattern else decimalCommaPattern
        
        input match
        {
            case regex(begin, integer, fract, scale, end) =>
            {
                val fraction = if(fract != null) fract.substring(1) else ""
                val trailingZeros = "0" * (scales(scale.toLowerCase) - fraction.length)
                begin + integer.replace(thousands, "") + fraction + trailingZeros + end
            }
            case _ => input
        }
    }
}
