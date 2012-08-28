package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.config.dataparser.ParserUtilsConfig
import java.text.{NumberFormat,DecimalFormatSymbols}
import org.dbpedia.extraction.util.Language

/**
 * Utility functions used by the data parsers.
 */
//TODO test after re-factor
class ParserUtils( context : { def language : Language } )
{
    private val scales = ParserUtilsConfig.scalesMap.getOrElse(context.language.wikiCode, ParserUtilsConfig.scalesMap("en"))

    // NumberFormat is not thread-safe
    private val numberFormat = new ThreadLocal[NumberFormat] {
      override def initialValue = NumberFormat.getNumberInstance(context.language.locale)
    } 
    
    private val groupingSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getGroupingSeparator
    
    private val decimalSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getDecimalSeparator

    // TODO: use "\s+" instead of "\s?" between number and scale?
    // TODO: in some Asian languages, digits are not separated by thousands but by ten thousands or so...
    private val regex = ("""(?i)([\D]*)([0-9]+(?:\""" + groupingSeparator + """[0-9]{3})*)(\""" + decimalSeparator + """[0-9]+)?\s?\[?\[?(""" + scales.keySet.mkString("|") + """)\]?\]?(.*)""").r
    
    def parse(str: String): Number = {
      // space is sometimes used as grouping separator
      numberFormat.get.parse(str.replace(' ', groupingSeparator))
    }

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
