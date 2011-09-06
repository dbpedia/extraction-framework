package org.dbpedia.extraction.wikiparser

import impl.wikipedia.Namespaces
import java.util.Locale
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils._
import java.net.URLEncoder

/**
 * Represents a page title.
 *
 * @param decoded Encoded page name. URL-decoded, using normalized spaces (not underscores), first letter uppercase.
 * @param namespace Namespace
 * @param language Language
 */
class WikiTitle(val decoded : String, val namespace : WikiTitle.Namespace = WikiTitle.Namespace.Main, val language : Language = Language.Default, val isInterlanguageLink : Boolean = false)
{
    if (decoded.isEmpty) throw new WikiParserException("page name must not be empty")

    /** Encoded page name (without namespace) e.g. Automobile_generation */
    lazy val encoded = WikiUtil.wikiEncode(decoded, language)

    /** Decoded page name with namespace e.g. Template:Automobile generation */
    def decodedWithNamespace =
    {
        if(namespace != WikiTitle.Namespace.Main)
        {
            WikiTitle.getNamespaceName(language, namespace) + ":" + decoded
        }
        else
        {
            decoded
        }
    }

    /** Encoded page name with namespace e.g. Template:Automobile_generation */
    def encodedWithNamespace =
    {
        if(namespace != WikiTitle.Namespace.Main)
        {
            val encodedNamespace = URLEncoder.encode(WikiTitle.getNamespaceName(language, namespace), "UTF-8")
            encodedNamespace + ":" + encoded
        }
        else
        {
            encoded
        }
    }
    
    /**
     * Returns the full source URI.
     */
    def sourceUri = "http://" + language.filePrefix + ".wikipedia.org/wiki/"  + encoded
    
    override def toString = language + ":" + decodedWithNamespace

    override def equals(other : Any) = other match
    {
        case otherTitle : WikiTitle => (namespace == otherTitle.namespace && decoded == otherTitle.decoded)
        case _ => false
    }

    override def hashCode = decoded.hashCode
}

object WikiTitle
{
    /**
     * Namespaces
     * 
     * see http://en.wikipedia.org/wiki/Wikipedia:Namespace
     * and http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/Defines.php
     */
    object Namespace extends Enumeration
    {
        val Special = Value(-1)
        val Media = Value(-2)
  
        val Main = Value(0)
        val Talk = Value(1)
        val User = Value(2)
        val UserTalk = Value(3)
        val Project = Value(4)
        val ProjectTalk = Value(5)
        val File = Value(6)
        val FileTalk = Value(7)
        val MediaWiki = Value(8)
        val MediaWikiTalk = Value(9)
        val Template = Value(10)
        val TemplateTalk = Value(11)
        val Help = Value(12)
        val HelpTalk = Value(13)
        val Category = Value(14)
        val CategoryTalk = Value(15)

        val Portal = Value(100)
        val PortalTalk = Value(101)
        val Author = Value(102)
        val AuthorTalk = Value(103)
        val Page = Value(104)
        val PageTalk = Value(105)
        val Index = Value(106)
        val IndexTalk = Value(107)
        val Book = Value(108)
        val BookTalk = Value(109)

        val Wikipedia = Value(150)

        val OntologyClass = Value(200)
        val OntologyProperty = Value(202)
        val Mapping = Value(204)
        val Mapping_de = Value(208)
        val Mapping_fr = Value(210)
        val Mapping_it = Value(212)
        val Mapping_es = Value(214)
        val Mapping_nl = Value(216)
        val Mapping_pt = Value(218)
        val Mapping_pl = Value(220)
        val Mapping_ru = Value(222)
        val Mapping_ca = Value(226)
        val Mapping_hu = Value(238)
        val Mapping_ko = Value(242)
        val Mapping_tr = Value(246)
        val Mapping_sl = Value(268)
        val Mapping_hr = Value(284)
        val Mapping_el = Value(304)
        val Mapping_ga = Value(396)
        

        def mappingNamespace(language : Language) : Option[Namespace] =
        {
            language.wikiCode match
            {
                case "en" => Some(Namespace.Mapping)
                case "de" => Some(Namespace.Mapping_de)
                case "fr" => Some(Namespace.Mapping_fr)
                case "it" => Some(Namespace.Mapping_it)
                case "es" => Some(Namespace.Mapping_es)
                case "nl" => Some(Namespace.Mapping_nl)
                case "pt" => Some(Namespace.Mapping_pt)
                case "pl" => Some(Namespace.Mapping_pl)
                case "ru" => Some(Namespace.Mapping_ru)
                case "ca" => Some(Namespace.Mapping_ca)
                case "hu" => Some(Namespace.Mapping_hu)
                case "ko" => Some(Namespace.Mapping_ko)
                case "tr" => Some(Namespace.Mapping_tr)
                case "sl" => Some(Namespace.Mapping_sl)
                case "hr" => Some(Namespace.Mapping_hr)
                case "el" => Some(Namespace.Mapping_el)
                case "ga" => Some(Namespace.Mapping_ga)
                case _ => None
            }
        }
    }
    type Namespace = Namespace.Value

    private val customNamespaces = Map[String, Namespace] (
        "OntologyClass" -> Namespace.OntologyClass,
        "OntologyProperty" -> Namespace.OntologyProperty,
        "Mapping" -> Namespace.Mapping,
        "Mapping de" -> Namespace.Mapping_de,
        "Mapping fr" -> Namespace.Mapping_fr,
        "Mapping it" -> Namespace.Mapping_it,
        "Mapping es" -> Namespace.Mapping_es,
        "Mapping nl" -> Namespace.Mapping_nl,
        "Mapping pt" -> Namespace.Mapping_pt,
        "Mapping pl" -> Namespace.Mapping_pl,
        "Mapping ru" -> Namespace.Mapping_ru,
        "Mapping ca" -> Namespace.Mapping_ca,
        "Mapping hu" -> Namespace.Mapping_hu,
        "Mapping ko" -> Namespace.Mapping_ko,
        "Mapping tr" -> Namespace.Mapping_tr,
        "Mapping sl" -> Namespace.Mapping_sl,
        "Mapping hr" -> Namespace.Mapping_hr,
        "Mapping el" -> Namespace.Mapping_el,
        "Mapping ga" -> Namespace.Mapping_ga
    )

    private val reverseCustomNamespaces = customNamespaces.map{case (name, code) => (code, name)}.toMap

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

        //Check if this is a interlanguage link (!beginning with ':')
        if(!parts.isEmpty && parts.head == "")
        {
            leadingColon = true
            parts = parts.tail
        }

        //Check if it contains a language
        if(!parts.isEmpty && !parts.tail.isEmpty)
        {
            Language.fromWikiCode(parts.head.toLowerCase) match
            {
                case Some(lang) =>
                {
                     language = lang
                     isInterlanguageLink = !leadingColon
                     parts = parts.tail
                }
                case None =>
            }
        }

        //Check if it contains a namespace
        if(!parts.isEmpty && !parts.tail.isEmpty)
        {
            getNamespace(language, parts.head) match
            {
                case Some(ns) =>
                {
                     namespace = ns
                     parts = parts.tail
                }
                case None =>
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

    private def getNamespace(language : Language, name : String) : Option[Namespace] =
    {
        val normalizedName = name.capitalizeLocale(Locale.ENGLISH)

        for(namespace <- customNamespaces.get(normalizedName))
        {
            return Some(namespace)
        }

        for(namespace <- Namespaces(language, normalizedName))
        {
            return Some(Namespace(namespace))
        }

        None
    }

    private def getNamespaceName(language : Language, code : Namespace) : String =
    {
        for(name <- reverseCustomNamespaces.get(code))
        {
            return name
        }

        Namespaces.getNameForNamespace(language, code)
    }
}
