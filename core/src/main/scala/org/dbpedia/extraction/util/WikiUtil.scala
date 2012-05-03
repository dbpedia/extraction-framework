package org.dbpedia.extraction.util

import java.net.URLEncoder
import java.net.URLDecoder
import org.dbpedia.extraction.util.RichString.toRichString

/**
 * Contains several utility functions related to WikiText.
 */
object WikiUtil
{
    /**
     * replace underscores by spaces, replace non-breaking space by normal space, 
     * normalize duplicate spaces, trim spaces from start and end
     * TODO: also remove exotic whitespace like \u200C\u200E\u200F\u2028
     * See WikiTitle.replace() and its use in WikiTitle.parse().
     * @param string string possibly using '_' instead of ' '
     */
    def cleanSpace( string : String ) : String =
    {
        string.replaceChars("_\u00A0", " \u0020").replaceAll(" +", " ").trim
    }
    
    /**
     * All of the following names will be encoded to '%C3%89mile_Zola': 
     * 'Émile Zola', 'émile Zola', 'Émile_Zola', ' Émile  Zola ', '  Émile _ Zola  '
     * 
     * TODO: maybe we should expect (require) the name to be normalized, e.g. with uppercase
     * first letter and without duplicate spaces or spaces at start or end? Would make this
     * method much simpler.
     * 
     * TODO: This method was a mistake. We should use specialized objects for resource identifiers, 
     * not Strings, and serialize them any way we want (IRI, URI, String, whatever).
     *   
     * @param name Non-encoded MediaWiki page name, e.g. 'Émile Zola'.
     * Must not include the namespace (e.g. 'Template:').
     */
    def wikiEncode(name : String, language : Language, capitalize : Boolean) : String =
    {
        // replace spaces by underscores.
        // Note: MediaWiki apparently replaces only spaces by underscores, not other whitespace. 
        var encoded = name.replace(' ', '_');
        
        // normalize duplicate underscores
        encoded = encoded.replaceAll("_+", "_");
        
        // trim underscores from start 
        encoded = encoded.replaceAll("^_", "");
        
        // trim underscores from end 
        encoded = encoded.replaceAll("_$", "");

        // make first character uppercase
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt. 
        // Example: [[istanbul]] generates a link to İstanbul (dot on the I) on tr.wikipedia.org
        // capitalize can be false for encoding property names, e.g. in the InfoboxExtractor
        if(capitalize)
        {
            encoded = encoded.capitalize(language.locale)
        }

        encoded.uriEscape("\"#%<>?[\\]^`{|}")
    }
    
    /**
     * All of the following names will be encoded to 'Émile Zola': 
     * '%C3%89mile_Zola', '%C3%A9mile_Zola', ' %C3%A9mile Zola ', ' %C3%A9mile _ Zola ', '  Émile _ Zola  '
     * 
     * TODO: maybe we should expect (require) the name to be normalized, e.g. with uppercase
     * first letter and without duplicate spaces or spaces at start or end? 
     * Would make this method much simpler.
     *   
     * @param name encoded MediaWiki page name, e.g. '%C3%89mile_Zola'.
     * Must not include the namespace (e.g. 'Template:').
     */
    def wikiDecode(name : String, language : Language, capitalize : Boolean) : String =
    {
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt.
        // Example: [[istanbul]] generates a link to İstanbul (dot on the I) on tr.wikipedia.org
        var decoded = cleanSpace(URLDecoder.decode(name, "UTF-8"))

        if(capitalize)
        {
            decoded = decoded.capitalize(language.locale)
        }

        decoded
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
