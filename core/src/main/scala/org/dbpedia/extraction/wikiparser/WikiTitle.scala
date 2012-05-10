package org.dbpedia.extraction.wikiparser

import java.util.Locale
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.RichString.toRichString
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.dbpedia.util.text.ParseExceptionIgnorer
import org.dbpedia.util.text.uri.UriDecoder
import scala.collection.mutable.ListBuffer

/**
 * Represents a page title.
 *
 * @param decoded Canonical page name: URL-decoded, using normalized spaces (not underscores), first letter uppercase.
 * @param namespace Namespace used to be optional, but that leads to mistakes
 * @param language Language used to be optional, but that leads to mistakes
 */
class WikiTitle (val decoded : String, val namespace : Namespace, val language : Language, val isInterLanguageLink : Boolean = false, val fragment : String = null)
{
    if (decoded.isEmpty) throw new WikiParserException("page name must not be empty")

    /** Wiki-encoded page name (without namespace) e.g. Automobile_generation */
    val encoded = WikiUtil.wikiEncode(decoded).capitalize(language.locale)

    /** Canonical page name with namespace e.g. "Template talk:Automobile generation" */
    val decodedWithNamespace = withNamespace(false)

    /** Wiki-encoded page name with namespace e.g. "Template_talk:Automobile_generation" */
    val encodedWithNamespace = withNamespace(true)
    
    /** page IRI for this page title */
    val pageIri = language.baseUri+"/wiki/"+encodedWithNamespace
    
    private def withNamespace(encode : Boolean) : String =
    {
      var ns = namespace.getName(language)
      if (encode) ns = WikiUtil.wikiEncode(ns).capitalize(language.locale)
      (if (ns isEmpty) ns else ns+':') + (if (encode) encoded else decoded)
    }
    
    /**
     * Returns useful info.
     */
    override def toString() = {
      val frag = if (fragment == null) "" else ";fragment='"+fragment+"'"
      "title="+decoded+";ns="+namespace+"/"+namespace.getName(language)+";language:"+language+frag;
    }

    /**
     * TODO: also use fragment?
     */
    override def equals(other : Any) = other match
    {
        case title : WikiTitle => (language == title.language && namespace == title.namespace && decoded == title.decoded)
        case _ => false
    }

    /**
     * TODO: do as Josh says in Effective Java, chapter 3.
     * TODO: also use fragment?
     */
    override def hashCode() = language.hashCode ^ decoded.hashCode ^ namespace.hashCode
}
    
object WikiTitle
{
    /**
     * Parses a MediaWiki link or title.
     * 
     * FIXME: parsing mediawiki links correctly cannot be done without a lot of configuration.
     * Therefore, this method must not be static. It must be part of an object that is instatiated
     * for each mediawiki instance.
     * 
     * FIXME: rules for links are different from those for titles. We should have distinct methods
     * for these two use cases.
     * 
     * @param link MediaWiki link e.g. "Template:Infobox Automobile"
     * @param sourceLanguage The source language of this link
     */
    def parse(title : String, sourceLanguage : Language) =
    {
        val coder = new HtmlCoder(XmlCodes.NONE)
        coder.setErrorHandler(ParseExceptionIgnorer.INSTANCE)
        var decoded = coder.code(title)
        
        // Note: Maybe the following line decodes too much, but it seems to be 
        // quite close to what MediaWiki does.
        decoded = UriDecoder.decode(decoded)
        
        // replace NBSP by SPACE, remove exotic whitespace
        // TODO: better treatment of U+20xx: remove some, replace some by space, others by LF
        decoded = decoded.replaceChars("\u00A0\u200C\u200D\u200E\u200F\u2028\u202A\u202B\u202C\u3000", " ")
        
        var fragment : String = null
        
        // we can look for hash signs after we decode - that's what MediaWiki does
        val hash = decoded.indexOf('#')
        if (hash != -1) {
          // TODO: check if we treat fragments correctly
          fragment = WikiUtil.cleanSpace(decoded.substring(hash + 1))
          decoded = decoded.substring(0, hash)
        }
        
        decoded = WikiUtil.cleanSpace(decoded)
        
        // FIXME: use interwiki prefixes from WikiSettingsDownloader.scala, e.g. [[q:Foo]] links to wikiquotes

        var parts = decoded.split(":", -1)

        var leadingColon = false
        var isInterLanguageLink = false
        var language = sourceLanguage
        var namespace = Namespace.Main

        //Check if this is an interlanguage link (beginning with ':')
        if(parts.length > 0 && parts.head == "")
        {
            leadingColon = true
            parts = parts.tail
        }

        //Check if it contains a language
        if (parts.length > 1)
        {
            for (lang <- Language.get(parts(0).trim.toLowerCase(sourceLanguage.locale)))
            {
                 language = lang
                 isInterLanguageLink = ! leadingColon
                 parts = parts.tail
            }
        }

        //Check if it contains a namespace
        if (parts.length > 1)
        {
            for (ns <- Namespace.get(language, parts(0).trim))
            {
                 namespace = ns
                 parts = parts.tail
            }
        }

        //Create the title name from the remaining parts
        // FIXME: MediaWiki doesn't capitalize links to other wikis
        val decodedName = parts.mkString(":").trim.capitalize(sourceLanguage.locale)

        new WikiTitle(decodedName, namespace, language, isInterLanguageLink, fragment)
    }
    
}
