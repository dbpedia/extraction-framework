package org.dbpedia.extraction.util

import java.util.Locale
import util.matching.Regex
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
}
