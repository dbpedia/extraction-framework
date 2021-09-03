package org.dbpedia.extraction.util

import org.dbpedia.extraction.util.RichString.wrapString
import java.util.Arrays

import org.dbpedia.iri.UriDecoder

/**
 * Contains several utility functions related to WikiText.
 */
object WikiUtil
{
    /**
     * replace underscores by spaces, replace non-breaking space by normal space, remove 
     * exotic whitespace, normalize duplicate spaces, trim whitespace (any char <= U+0020) 
     * from start and end.
     * 
     * Also see WikiTitle.parse().
     * 
     * TODO: better treatment of U+20xx: remove some, replace some by space, others by LF
     * 
     * FIXME: There is no logic to our decoding / encoding of strings, URIs, etc. It's done 
     * in too many places. We must set a policy and use distinct classes, not generic strings.
     * 
     * @param name string possibly using '_' instead of ' '
     */
    def cleanSpace(name: String): String =
    {
      // FIXME: removing these chars may avoid some errors, but also introduces others.
      // For example, the local name of the 'Project' namespace on fa wikipedia contains U+200C.
      //name.replaceChars("_\u00A0\u200E\u200F\u2028\u202A\u202B\u202C\u3000", " ").replaceAll(" +", " ").trim

      val replacementChar = ' '
      val replacemanrMap: Map[Char, Char] = Map(
        '_' -> replacementChar,
        '\u00A0' -> replacementChar,
        '\u200E' -> replacementChar,
        '\u200F' -> replacementChar,
        '\u2028' -> replacementChar,
        '\u202A' -> replacementChar,
        '\u202B' -> replacementChar,
        '\u202C' -> replacementChar,
        '\u3000' -> replacementChar
      )
        val sb = new StringBuilder()
        val chars = name.toCharArray

        var pos = 0
        var l = replacementChar                         // since l is a ' ' any prefix underscores/whitespce will be ignored (replaceAll("^_+"))

        while (pos < chars.length)
        {
          val c = chars(pos)
          replacemanrMap.get(c) match{
            case Some(r) =>
              if(l != r)                // replaceAll(" +", " ")
                sb.append(r)
              l = r
            case None =>
              sb.append(c)
              l = c
          }
          pos += 1
        }
        sb.toString().trim
    }
    
    /**
     * Replacement string array for StringUtils.escape
      * SH: I added ^`?" not sure why they were removed in the first place
     */
    def iriReplacements: Array[String] = StringUtils.replacements('%', "#<>[]{}|\\^`?\"")
    
    // store our own private copy of the mutable array
    private val replacements = iriReplacements
    
    /**
     * Replaces multiple spaces (U+0020) by one, removes spaces from start and end, 
     * replaces spaces by underscores, and percent-encodes the following characters:
     *
      * TODO CENTRAL STRING MANAGEMENT
     * "#%<>?[\]^`{|}
     *
     * The result is usable in most parts of a IRI. The ampersand '&' is not escaped though.
     * 
     * Should only be used for canonical MediaWiki page names. Not for fragments, not for queries.
     * 
     * TODO: a canonical MediaWiki page name does not contain multiple spaces. We should not
     * clean spaces but simply throw an exception if the name is not canonical.
     * 
     * @param name Canonical MediaWiki page name, e.g. 'Émile Zola'
     */
    def wikiEncode(name : String): String =
    {
      // replace spaces by underscores.
      // Note: MediaWiki apparently replaces only spaces by underscores, not other whitespace.
      //var encoded = name.replace(' ', '_');
      
      // normalize duplicate underscores
      //encoded = encoded.replaceAll("_+", "_");
      
      // trim underscores from start 
      //encoded = encoded.replaceAll("^_+", "");

        // trim underscores from end
        //encoded = encoded.replaceAll("_+$", "");

        val sb = new StringBuilder()
        val chars = name.toCharArray

        var pos = 0
        var l = '_'                         // since l is a _ any prefix underscores/whitespce will be ignored (replaceAll("^_+"))

        while (pos < chars.length)
        {
            val c = chars(pos)
            if(c == '_' || c == ' ')        // replace(' ', '_')
            {
                if(l != '_')                // replaceAll("_+", "_")
                    sb.append('_')
                l = '_'
            }
            else
            {
                sb.append(c)
                l = c
            }
            pos += 1
        }

        val ret = sb.toString

        // replacing trailing underscore
        // escape all relevant characters
        if(ret.length == 0)
            ret
        else
            StringUtils.escape(if (l == '_') ret.substring(0, ret.length - 1) else ret, replacements)
    }
    
        
    /**
     * @param name encoded MediaWiki page name, e.g. '%C3%89mile_Zola'.
     * Must not include the namespace (e.g. 'Template:').
     */
    def wikiDecode(name : String) : String =
    {
        cleanSpace(UriDecoder.decode(name))
    }

    private val wikiEmphasisRegex1 = "(?s)'''''(.*?)'''''".r
    private val wikiEmphasisRegex2 = "(?s)'''(.*?)'''".r
    private val wikiEmphasisRegex3 = "(?s)''(.*?)''".r

    /**
     * Removes Wiki emphasis.
     *
     * @param text
     * @return The given text without the wiki emphasis
     */
    def removeWikiEmphasis(text : String) : String =
    {
        // note: I was tempted to replace these three by a single regex,
        // but it wouldn't really work.
        var result = wikiEmphasisRegex1.replaceAllIn(text, "$1")
        result = wikiEmphasisRegex2.replaceAllIn(result, "$1")
        result = wikiEmphasisRegex3.replaceAllIn(result, "$1")
        result
    }
}
