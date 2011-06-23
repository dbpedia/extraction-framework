package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.config.dataparser.ParserUtilsConfig
import java.text.NumberFormat
import org.dbpedia.extraction.util.Language

/**
 * Utility functions used by the data parsers.
 */
//TODO test after re-factor
class ParserUtils( extractionContext : { val language : Language } )
{
    private val scales = ParserUtilsConfig.scalesMap.getOrElse(extractionContext.language.wikiCode, ParserUtilsConfig.scalesMap("en"))

    private val numberFormat = NumberFormat.getInstance(extractionContext.language.locale)
    private val thousandsSeparator = numberFormat.format(1000).charAt(1).toString
    private val decimalSeparator = numberFormat.format(0.1).charAt(1).toString

    // TODO: use "\s+" instead of "\s?" between number and scale?
    // TODO: in some Asian languages, digits are not separated by thousands but by ten thousands or so...
    private val regex = ("""(?i)([\D]*)([0-9]+(?:\""" + thousandsSeparator + """[0-9]{3})*)(\""" + decimalSeparator + """[0-9]+)?\s?\[?\[?(""" + scales.keySet.mkString("|") + """)\]?\]?(.*)""").r

    /**
     * Converts large numbers like '100.5 million' to '100500000'
     */
    def convertLargeNumbers(input : String) : String =
    {
        input match
        {
            case regex(begin, integer, fract, scale, end) =>
            {
                val fraction = if(fract != null) fract.substring(1) else ""
                val trailingZeros = "0" * (scales(scale.toLowerCase) - fraction.length)
                begin + integer/*.replace(thousandsSeparator, "")*/ + fraction + trailingZeros + end
            }
            case _ => input
        }
    }
}
