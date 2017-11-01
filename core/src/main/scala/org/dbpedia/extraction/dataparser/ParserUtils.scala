package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.config.dataparser.ParserUtilsConfig
import java.text.{NumberFormat,DecimalFormatSymbols}
import org.dbpedia.extraction.util.Language
import java.util.Locale
import java.util.regex.Pattern
import scala.language.reflectiveCalls

/**
 * Utility functions used by the data parsers.
 */
//TODO test after re-factor
class ParserUtils( val context : { def language : Language } )
{
    private val scales = ParserUtilsConfig.scalesMap.getOrElse(context.language.wikiCode, ParserUtilsConfig.scalesMap("en"))

    // NumberFormat is not thread-safe
    private val numberFormat = new ThreadLocal[NumberFormat] {
      override def initialValue = NumberFormat.getNumberInstance(context.language.locale)
    } 
    
    private val groupingSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getGroupingSeparator
    
    private val defaultDecimalSeparator = DecimalFormatSymbols.getInstance(context.language.locale).getDecimalSeparator
    private val decimalSeparatorsRegex = ParserUtilsConfig.decimalSeparators.get(context.language.wikiCode) match
      {
        case Some(sep) => ("["+sep+"]").r
        case None => ("""\"""+defaultDecimalSeparator).r 
      }

    private val scalesRegex = scales.keySet.map(Pattern.quote).toList.sortWith((a,b) => a.length > b.length).mkString("|")
    // TODO: use "\s+" instead of "\s?" between number and scale?
    // TODO: in some Asian languages, digits are not separated by thousands but by ten thousands or so...
    private val regex = ("""(?i)([\D]*)([0-9]+(?:\""" + groupingSeparator + """[0-9]{3})*)(""" + decimalSeparatorsRegex + """[0-9]+)?\s?\[?\[?(""" + scalesRegex + """)\]?\]?(.*)""").r

  private[dataparser] def parse(str: String): Number = {
      // space is sometimes used as grouping separator
      val cleanedString = decimalSeparatorsRegex.replaceAllIn(str, ""+defaultDecimalSeparator)
      numberFormat.get.parse(cleanedString.replace(' ', groupingSeparator))
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
