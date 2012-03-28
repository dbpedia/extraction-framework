package org.dbpedia.extraction.wikiparser

import java.util.Locale
import scala.collection.mutable.HashMap
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils._
import java.net.URLEncoder
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

/**
 * Represents a page title.
 *
 * @param decoded Encoded page name. URL-decoded, using normalized spaces (not underscores), first letter uppercase.
 * @param namespace Namespace used to be optional, but that leads to mistakes
 * @param language Language used to be optional, but that leads to mistakes
 */
class WikiTitle(val decoded : String, val namespace : Namespace, val language : Language, val isInterlanguageLink : Boolean = false)
{
    if (decoded.isEmpty) throw new WikiParserException("page name must not be empty")

    /** Encoded page name (without namespace) e.g. Automobile_generation */
    val encoded = WikiUtil.wikiEncode(decoded, language)

    /** Decoded page name with namespace e.g. Template:Automobile generation */
    val decodedWithNamespace = withNamespace(false)

    /** Encoded page name with namespace e.g. Template:Automobile_generation */
    val encodedWithNamespace = withNamespace(true)
    
    private def withNamespace(encode : Boolean) : String =
    {
        val name : String = if (encode) encoded else decoded
        if (namespace == Namespace.Main)
        {
          name
        }
        else
        {
          val ns = Namespace.getNamespaceName(language, namespace)
          (if (encode) WikiUtil.wikiEncode(ns, language) else ns)+ ":" + name
        }
    }
    
    /**
     * Returns the full source URI.
     */
    val sourceUri = "http://" + language.wikiCode + ".wikipedia.org/wiki/"  + encodedWithNamespace
    
    /**
     * TODO: the string is confusing. For a half hour I thought that the titles I saw in a log file
     * were the ones used by Wikipedia and was baffled. I think we should change this method to
     * return something like decodedWithNamespace+" ["+language+"]".
     * 
     * Problem: find out if some code relies on the current result format of this method 
     * and fix that code. Then change this method.
     */
    override def toString() = language + ":" + decodedWithNamespace

    /**
     * FIXME: this method must also take into account the language. Problem: find out if some code 
     * relies on the current behavior of this method and fix that code. Then change this method.
     * Also change hashCode.
     */
    override def equals(other : Any) = other match
    {
        case otherTitle : WikiTitle => (namespace == otherTitle.namespace && decoded == otherTitle.decoded)
        case _ => false
    }

    /**
     * TODO: when equals() is fixed, also use language here.
     * TODO: do as Josh says in Effective Java, chapter 3.
     */
    override def hashCode() = decoded.hashCode ^ namespace.hashCode
}
    
object WikiTitle
{

    /**
     * Parses a (decoded) MediaWiki link
     * @param link MediaWiki link e.g. "Template:Infobox Automobile"
     * @param sourceLanguage The source language of this link
     */
    def parse(link : String, sourceLanguage : Language = Language.Default) =
    {
        // TODO: handle special prefixes, e.g. [[q:Foo]] links to WikiQuotes

        var parts = link.split(":", -1).toList

        var leadingColon = false
        var isInterlanguageLink = false
        var language = sourceLanguage
        var namespace = Namespace.Main

        //Check if this is a interlanguage link (beginning with ':')
        if(!parts.isEmpty && parts.head == "")
        {
            leadingColon = true
            parts = parts.tail
        }

        //Check if it contains a language
        if(!parts.isEmpty && !parts.tail.isEmpty)
        {
            for (lang <- Language.get(parts.head.toLowerCase(sourceLanguage.locale)))
            {
                 language = lang
                 isInterlanguageLink = !leadingColon
                 parts = parts.tail
            }
        }

        //Check if it contains a namespace
        if(!parts.isEmpty && !parts.tail.isEmpty)
        {
            for (ns <- Namespace.getNamespace(language, parts.head))
            {
                 namespace = ns
                 parts = parts.tail
            }
        }

        //Create the title name from the remaining parts
        val decodedName = WikiUtil.cleanSpace(parts.mkString(":")).capitalizeLocale(sourceLanguage.locale)

        new WikiTitle(decodedName, namespace, language, isInterlanguageLink)
    }

    /**
     * Parses an encoded MediaWiki link
     * @param link encoded MediaWiki link e.g. "Template:Infobox_Automobile"
     * @param sourceLanguage The source language of this link
     */
    def parseEncoded(encodedLink : String, sourceLanguage : Language = Language.Default) : WikiTitle =
    {
        parse(WikiUtil.wikiDecode(encodedLink, sourceLanguage), sourceLanguage)
    }

}
