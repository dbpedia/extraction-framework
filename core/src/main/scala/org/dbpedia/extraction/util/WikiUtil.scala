package org.dbpedia.extraction.util

import java.net.URLEncoder
import java.net.URLDecoder
import org.dbpedia.extraction.util.StringUtils._

/**
 * Contains several utility functions related to WikiText.
 */
object WikiUtil
{
    /**
     * replace underscores by spaces, normalize duplicate spaces, trim spaces from start and end
     * @param string string using '_' instead of ' '
     */
    def cleanSpace( string : String ) : String =
    {
        string.replace('_', ' ').replaceAll(" +", " ").trim
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
    def wikiEncode(name : String, language : Language = Language.Default, capitalize : Boolean = true) : String =
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
            encoded = encoded.capitalizeLocale(language.locale)
        }

        // URL-encode everything but ':' '/' '&' and ',' - just like MediaWiki
        // TODO: MediaWiki probably never did it like that, don't know where I got that from.
        // See http://svn.wikimedia.org/viewvc/mediawiki/trunk/phase3/includes/GlobalFunctions.php?r1=38683&r2=38908
        // I think we're free to do as we choose as long as we produce valid URIs.
        // jc@sahnwaldt.de 2012-03-05
        encoded = URLEncoder.encode(encoded, "UTF-8");
        encoded = encoded.replace("%3A", ":");
        encoded = encoded.replace("%2F", "/");
        encoded = encoded.replace("%26", "&");
        encoded = encoded.replace("%2C", ",");

        encoded;
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
    def wikiDecode(name : String, language : Language = Language.Default, capitalize : Boolean = true) : String =
    {
        // Capitalize must be Locale-specific. We must use a different method for languages tr, az, lt.
        // Example: [[istanbul]] generates a link to İstanbul (dot on the I) on tr.wikipedia.org
        var decoded = cleanSpace(URLDecoder.decode(name, "UTF-8"))

        if(capitalize)
        {
            decoded = decoded.capitalizeLocale(language.locale)
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
