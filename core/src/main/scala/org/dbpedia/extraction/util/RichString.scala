package org.dbpedia.extraction.util

import java.util.Locale
import java.lang.StringBuilder
import scala.util.matching.Regex
import RichString.toRichString
import java.util.regex.{Pattern,Matcher}

/**
 * Defines additional methods on strings, which are missing in the standard library.
 */
object RichString
{
    implicit def toRichString(str : String) = new RichString(str)
}

/**
 * Defines additional methods on strings, which are missing in the standard library.
 */
class RichString(str : String)
{
    /**
     * Converts the first character in this String to upper case using the rules of the given Locale.
     * Does not convert a 'ß' at the start of the string to 'SS'.
     * TODO: there are probably other cases like 'ß' that should not be capitalized
     * 
     * @param locale The locale used to capitalize the String
     */
    def capitalize(locale : Locale) : String = {
      if (str.isEmpty) "" 
      else if (str.startsWith("ß")) str 
      else str.head.toString.toUpperCase(locale) + str.tail
    }

    /**
     * Converts the first character in this String to lower case using the rules of the given Locale.
     *
     * @param locale The locale
     */
    def uncapitalize(locale : Locale) : String = {
        if (! str.isEmpty) str.head.toString.toLowerCase(locale) + str.tail else ""
    }

    /**
     * Converts this String to camel case. The first character is not changed.
     *
     * @param splitAt The regex used to split the string into words
     * @param locale The locale used to capitalize the words
     */
    def toCamelCase(splitAt : Regex, locale : Locale) : String = {
        val words = splitAt.split(str)
        if (words.isEmpty) return ""
        words.head + words.tail.map(word => word.capitalize(locale)).mkString
    }
    
    /**
     * Return a copy of this string in which all occurrences of chars from the given string
     * have been converted to UTF-8 hex representation where each two-digit hex byte is prefixed
     * by the given escape character.
     * 
     * TODO: This method does not work for code points > 0xFFFF.
     * 
     * @param chars list of characters that should be escaped if they occur in the string.
     * 
     * TODO: this method is pretty inefficient. It is usually used with the same chars all the 
     * time, so we should have an array containing their escaped values and use a lookup table.
     */
    def escape(esc: Char, chars : String): String = {
      val sb = StringUtils.escape(null, str, esc, chars)
      if (sb == null) str else sb.toString
    }
    
    /**
     * return a copy of this string in which all occurrences of chars from the first string
     * have been replaced by the corresponding char from the second string. If there is no
     * corresponding char (i.e. the second string is shorter than the first one), the affected
     * char is removed.
     * 
     * TODO: This method does not work for code points > 0xFFFF.
     * 
     * @param chars list of characters that should be replaced if they occur in the string.
     */
    def replaceChars(chars: String, replace: String) : String = {
      val sb = StringUtils.replaceChars(null, str, chars, replace)
      if (sb == null) str else sb.toString
    }
    
    /**
     * Similar to java.lang.String.replaceAll() and scala.util.matching.Regex.replaceAllIn(),
     * but the replacement can be chosen more flexibly and this method should be more efficient.
     */
    def replaceBy(pattern: Pattern, append: (StringBuilder, Matcher) => _): String = {
      val matcher = pattern.matcher(str)
      if (! matcher.find()) return str
      val sb = new StringBuilder
      var last = 0
      do {
        sb.append(str, last, matcher.start)
        append(sb, matcher)
        last = matcher.end
      } while (matcher.find())
      sb.append(str, last, str.length)
      sb.toString
    }
    
}
