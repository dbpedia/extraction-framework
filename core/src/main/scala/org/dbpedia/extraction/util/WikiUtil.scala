package org.dbpedia.extraction.util

import org.dbpedia.extraction.util.RichString.toRichString
import org.dbpedia.util.text.uri.UriDecoder

/**
 * Contains several utility functions related to WikiText.
 */
object WikiUtil
{
    /**
     * replace underscores by spaces, replace non-breaking space by normal space, 
     * normalize duplicate spaces, trim whitespace (any char <= U+0020) from start and end.
     * 
     * TODO: remove or replace exotic whitespace like U+200C, U+200E, U+200F, U+2028?
     * 
     * See WikiTitle.replace() and its use in WikiTitle.parse().
     * 
     * FIXME: There is no logic to our decoding / encoding of strings, URIs, etc. It's done 
     * in too many places. We must set a policy and use distinct classes, not generic strings.
     * 
     * @param string string possibly using '_' instead of ' '
     */
    def cleanSpace(string: String): String =
    {
        string.replaceChars("_\u00A0", "  ").replaceAll(" +", " ").trim
    }
    
    private val iriReplacements = StringUtils.replacements('%', "\"#%<>?[\\]^`{|}")
    
    /**
     * Replaces multiple spaces (U+0020) by one, removes spaces from start and end, 
     * replaces spaces by underscores, and percent-encodes the following characters:
     * 
     * "#%<>?[\\]^`{|}
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
      // TODO: all this replacing is inefficient, one loop over the string would be nicer.
      
      // replace spaces by underscores.
      // Note: MediaWiki apparently replaces only spaces by underscores, not other whitespace.
      var encoded = name.replace(' ', '_');
      
      // normalize duplicate underscores
      encoded = encoded.replaceAll("_+", "_");
      
      // trim underscores from start 
      encoded = encoded.replaceAll("^_", "");
      
      // trim underscores from end 
      encoded = encoded.replaceAll("_$", "");

      val sb = StringUtils.escape(null, encoded, iriReplacements)
      
      if (sb == null) encoded else sb.toString
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
