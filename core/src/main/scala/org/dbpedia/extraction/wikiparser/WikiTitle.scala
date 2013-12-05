package org.dbpedia.extraction.wikiparser

import java.util.Locale
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.dbpedia.util.text.ParseExceptionIgnorer
import org.dbpedia.util.text.uri.UriDecoder
import scala.collection.mutable.ListBuffer

/**
 * Represents a page title. Or a link to a page.
 *
 * FIXME: a link is different from a title and should be represented by a different class.
 *
 * @param decoded Canonical page name: URL-decoded, using normalized spaces (not underscores), first letter uppercase.
 * @param namespace Namespace used to be optional, but that leads to mistakes
 * @param language Language used to be optional, but that leads to mistakes
 */
class WikiTitle (
  val decoded: String,
  val namespace: Namespace,
  val language: Language,
  val isInterLanguageLink: Boolean = false,
  val fragment: String = null
)
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

    /** resource IRI for this page title */
    val resourceIri = language.resourceUri.append(encodedWithNamespace)

    private def withNamespace(encode : Boolean) : String =
    {
      var ns = namespace.name(language)
      if (encode) ns = WikiUtil.wikiEncode(ns).capitalize(language.locale)
      (if (ns.isEmpty) ns else ns+':') + (if (encode) encoded else decoded)
    }

    /**
     * Returns useful info.
     */
    override def toString() = {
      val frag = if (fragment == null) "" else ";fragment='"+fragment+"'"
      "title="+decoded+";ns="+namespace+"/"+namespace.name(language)+";language:"+language+frag;
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

    /**
     * If somehow a different namespace is also given for this title, store it here. Otherwise,
     * this field is null.
     *
     * Why do we need this nonsense? http://gd.wikipedia.org/?curid=4184 and http://gd.wikipedia.org/?curid=4185&redirect=no
     * have the same title "Teamplaid:Gàidhlig", but they are in different namespaces: 4184 is in
     * the template namespace, 4185 is in the main namespace. It looks like MediaWiki can handle this
     * somehow, but when we try to import both pages from the XML dump file into the database,
     * MySQL rightly complains about the duplicate title. As a workaround, we simply reject pages
     * for which the <ns> namespace doesn't fit the <title> namespace.
     */
    var otherNamespace: Namespace = null
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
    def parse(title : String, sourceLanguage : Language): WikiTitle =
    {
        val coder = new HtmlCoder(XmlCodes.NONE)
        coder.setErrorHandler(ParseExceptionIgnorer.INSTANCE)
        var decoded = coder.code(title)

        // Note: Maybe the following line decodes too much, but it seems to be
        // quite close to what MediaWiki does.
        decoded = UriDecoder.decode(decoded)

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
          //When we have the same namespace as the wikicode it is not a language
          //This happens in wikidata, commons and wiktionary (for now)
          val p0 = parts(0).trim.toLowerCase(sourceLanguage.locale)
          if (!p0.equals(sourceLanguage.wikiCode))
          {
            for (lang <- Language.get(p0))
            {
              language = lang
              isInterLanguageLink = ! leadingColon
              parts = parts.tail
            }
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
