package org.dbpedia.extraction.util

import java.util.Locale
import util.matching.Regex

/**
 * Defines additional methods on strings, which are missing in the standard library.
 */
object StringUtils
{
    implicit def toStringUtils(str : String) = new StringUtils(str)

    /** 
     * Displays minutes, seconds and milliseconds.
     */
    def prettyMillis(millis : Long) = zeros(2, millis / 60000)+':'+zeros(2, millis % 60000 / 1000)+'.'+zeros(3, millis % 1000)+'s'
    
    private val zeros = "0000000000000000000000000"
    
    /**
     * Zeropad givn number.
     * undefined result for more than 20 or so zeros. May even loop forever because of integer overflow.
     */
    private def zeros(z: Int, n: Long) : String = {
        var p = 10
        var e = 1
        while (e < z) {
          if (n < p) return zeros.substring(0, z - e)+n
          p *= 10
          e += 1
        }
        n.toString
    }
    
    object IntLiteral
    {
        def apply(x : Int) = x.toString

        def unapply(x : String) : Option[Int] =  try { Some(x.toInt) } catch { case _ => None }
    }

    object BooleanLiteral
    {
        def apply(x : Boolean) = x.toString

        def unapply(x : String) : Option[Boolean] =  try { Some(x.toBoolean) } catch { case _ => None }
    }
}

/**
 * Defines additional methods on strings, which are missing in the standard library.
 */
class StringUtils(str : String)
{
    /**
     * Converts the first character in this String to upper case using the rules of the given Locale.
     *
     * @param locale The locale used to capitalize the String
     */
    def capitalize(locale : Locale) : String =
    {
        if (!str.isEmpty) str.head.toString.toUpperCase(locale) + str.tail else ""
    }

    /**
     * Converts the first character in this String to lower case using the rules of the given Locale.
     *
     * @param locale The locale
     */
    def uncapitalize(locale : Locale) : String =
    {
        if (!str.isEmpty) str.head.toString.toLowerCase(locale) + str.tail else ""
    }

    /**
     * Converts this String to camel case. The first character is not changed.
     *
     * @param splitAt The regex used to split the string into words
     * @param locale The locale used to capitalize the words
     */
    def toCamelCase(splitAt : Regex, locale : Locale) : String =
    {
        val words = splitAt.split(str)

        if (words.isEmpty) return ""

        words.head + words.tail.map(word => new StringUtils(word).capitalize(locale)).mkString
    }
}
