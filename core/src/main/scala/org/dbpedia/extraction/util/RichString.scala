package org.dbpedia.extraction.util

import java.util.Locale
import java.lang.StringBuilder
import scala.util.matching.Regex
import RichString.toRichString

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
     *
     * @param locale The locale used to capitalize the String
     */
    def capitalize(locale : Locale) : String = {
        if (! str.isEmpty) str.head.toString.toUpperCase(locale) + str.tail else ""
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
     * @param chars list of characters that should be percent-encoded if they occur in the string.
     * Must be ASCII characters, i.e. Unicode code points from U+0020 to U+007F (inclusive).
     * This method does not correctly escape characters outside that range.
     * TODO: this method is pretty inefficient. It is used with the same chars all the time, 
     * so we should have an array containing their escaped values and use a lookup table.
     */
    def uriEscape(chars : String) : String = {
        val sb = new StringBuilder
        for (c <- str) if (chars.indexOf(c) == -1) sb append c else sb append '%' append c.toInt.toHexString.toUpperCase(Locale.ENGLISH)
        sb.toString
    }      
    
    /**
     * @param chars list of characters that should be percent-encoded if they occur in the string.
     * Must be ASCII characters, i.e. Unicode code points from U+0020 to U+007F (inclusive).
     * This method does not correctly escape characters outside that range.
     * TODO: this method is pretty inefficient. It is used with the same chars all the time, 
     * so we should have an array containing their escaped values and use a lookup table.
     */
    def dotEscape(chars : String) : String = {
        val sb = new StringBuilder
        for (c <- str) if (chars.indexOf(c) == -1) sb append c else sb append '.' append c.toInt.toHexString.toUpperCase(Locale.ENGLISH)
        sb.toString
    }      
    
    /**
     * return a copy of this string in which all occurrences of chars from the first string
     * have been replaced by the corresponding char from the second string. If there is no
     * corresponding char (i.e. the second string is shorter than the first one), the affected
     * char is removed.
     * 
     * This method does not work for code points > 0xFFFF.
     */
    def replaceChars(chars : String, rep : String) : String = {
      replace(chars, (sb, index, ch) => if (index < rep.length) sb.append(rep.charAt(index)) )
    }

    /**
     * @param chars list of characters that should be percent-encoded if they occur in the string.
     * Must be ASCII characters, i.e. Unicode code points from U+0020 to U+007F (inclusive).
     * This method does not correctly escape characters outside that range.
     * TODO: this method is pretty inefficient. It is used with the same chars all the time, 
     * so we should have an array containing their escaped values and use a lookup table.
     */
    def escape(esc: Char, chars : String) : String = {
      replace(chars, (sb, index, ch) => sb append esc append ch.toInt.toHexString.toUpperCase(Locale.ENGLISH))
    }
    
    /**
     * @param chars list of characters that should be percent-encoded if they occur in the string.
     * Must be ASCII characters, i.e. Unicode code points from U+0020 to U+007F (inclusive).
     * This method does not correctly escape characters outside that range.
     * TODO: this method is pretty inefficient. It is used with the same chars all the time, 
     * so we should have an array containing their escaped values and use a lookup table.
     */
    private def replace(chars : String, append: (StringBuilder, Int, Char) => Unit ) : String = {
      
      var sb : StringBuilder = null
      var last = 0
      var pos = 0
      
      while (pos < str.length)
      {
        val ch = str.charAt(pos)
        val index = chars.indexOf(ch)
        if (index != -1)
        {
          if (sb == null) sb = new StringBuilder()
          sb.append(str, last, pos)
          append(sb, index, ch)
          last = pos + 1
        }
        
        pos += 1
      }
      
      if (sb != null) sb.append(str, last, str.length)
      if (sb == null) str else sb.toString
    }
    

}
