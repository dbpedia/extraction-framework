package org.dbpedia.extraction.wikiparser

import java.util.Locale
import java.lang.StringBuilder
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.dbpedia.util.text.ParseExceptionIgnorer
import org.dbpedia.util.text.uri.UriDecoder
import scala.collection.mutable.ListBuffer

/**
 * Represents a page title.
 *
 * @param decoded Encoded page name. URL-decoded, using normalized spaces (not underscores), first letter uppercase.
 * @param namespace Namespace used to be optional, but that leads to mistakes
 * @param language Language used to be optional, but that leads to mistakes
 */
class WikiTitle (val decoded : String, val namespace : Namespace, val language : Language, val isInterlanguageLink : Boolean = false, val fragment : String = null)
{
    if (decoded.isEmpty) throw new WikiParserException("page name must not be empty")

    /** Encoded page name (without namespace) e.g. Automobile_generation */
    val encoded = WikiUtil.wikiEncode(decoded, language, capitalize=true)

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
          val ns = namespace.getName(language)
          (if (encode) WikiUtil.wikiEncode(ns, language, capitalize=true) else ns)+ ":" + name
        }
    }
    
    /**
     * Returns the full source URI.
     */
    val sourceUri = "http://" + language.wikiCode + ".wikipedia.org/wiki/"  + encodedWithNamespace
    
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
        decoded = replace(decoded, "\u00A0\u200C\u200E\u200F\u2028\u202B\u202C\u3000", " ")
        
        var fragment : String = null
        
        // we can look for hash signs after we decode - that's what MediaWiki does
        val hash = decoded.indexOf('#')
        if (hash != -1) {
          // TODO: check if we treat fragments correctly
          fragment = WikiUtil.cleanSpace(decoded.substring(hash + 1))
          decoded = decoded.substring(0, hash)
        }
        
        decoded = WikiUtil.cleanSpace(decoded)
        
        // FIXME: handle special prefixes, e.g. [[q:Foo]] links to WikiQuotes
        // get them live from 
        // http://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap&format=xml
        // http://de.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap&format=xml
        // etc. Almost identical, except for stuff like q => en.wikiquote.org, de.wikiquote.org

        var parts = decoded.split(":", -1)

        var leadingColon = false
        var isInterlanguageLink = false
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
                 isInterlanguageLink = ! leadingColon
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

        new WikiTitle(decodedName, namespace, language, isInterlanguageLink, fragment)
    }
    
    /**
     * return a copy of the first string in which all occurrences of chars from the second string
     * have been replaced by the corresponding char from the third string. If there is no
     * corresponding char (i.e. the third string is shorter than the second one), the affected
     * char is removed.
     */
    private def replace(str : String, chars : String, replace : String) : String = 
    {
      var sb : StringBuilder = null
      var last = 0
      var pos = 0
      
      while (pos < str.length)
      {
        val index = chars.indexOf(str.charAt(pos))
        if (index != -1)
        {
          if (sb == null) sb = new StringBuilder()
          sb.append(str, last, pos)
          if (index < replace.length) sb.append(replace.charAt(index))
          last = pos + 1
        }
        
        pos += 1
      }
      
      if (sb != null) sb.append(str, last, str.length)
      if (sb == null) str else sb.toString
    }

}
