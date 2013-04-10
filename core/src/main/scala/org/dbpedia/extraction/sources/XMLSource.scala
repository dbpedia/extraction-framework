package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle
import java.io.{File,FileInputStream,InputStreamReader}
import scala.xml.Elem
import org.dbpedia.extraction.util.Language
import java.io.Reader

/**
 *  Loads wiki pages from an XML stream using the MediaWiki export format.
 *
 *  The MediaWiki export format is specified by
 *  http://www.mediawiki.org/xml/export-0.4
 *  http://www.mediawiki.org/xml/export-0.5
 *  http://www.mediawiki.org/xml/export-0.6
 *  http://www.mediawiki.org/xml/export-0.8
 *  etc.
 */
object XMLSource
{
    /**
     * Creates an XML Source from an input stream.
     *
     * @param stream The input stream to read from. Will be closed after reading.
     * @param filter Function to filter pages by their title. Pages for which this function returns false, won't be yielded by the source.
     * @param language if given, parser expects file to be in this language and doesn't read language from siteinfo element
     */
    def fromFile(file: File, language: Language, filter: WikiTitle => Boolean = (_ => true)) : Source = {
      fromReader(() => new InputStreamReader(new FileInputStream(file), "UTF-8"), language, filter)
    }

    /**
     * Creates an XML Source from a reader.
     *
     * @param stream The input stream to read from. Will be closed after reading.
     * @param filter Function to filter pages by their title. Pages for which this function returns false, won't be yielded by the source.
     * @param language if given, parser expects file to be in this language and doesn't read language from siteinfo element
     */
    def fromReader(source: () => Reader, language: Language, filter: WikiTitle => Boolean = (_ => true)) : Source = {
      new XMLReaderSource(source, language, filter)
    }

    /**
     *  Creates an XML Source from a parsed XML tree.
     *
     * @param xml The xml which contains the pages
     */
    def fromXML(xml : Elem, language: Language) : Source  = new XMLSource(xml, language)

    /**
       *  Creates an XML Source from a parsed XML OAI response.
       *
       * @param xml The xml which contains the pages
     */
    def fromOAIXML(xml : Elem) : Source  = new OAIXMLSource(xml)
}

/**
 * XML source which reads from a file
 */
private class XMLReaderSource(source: () => Reader, language: Language, filter: WikiTitle => Boolean) extends Source
{
    override def foreach[U](proc : WikiPage => U) : Unit = {
      val reader = source()
      try new WikipediaDumpParser(reader, language, filter.asInstanceOf[WikiTitle => java.lang.Boolean], proc).run()
      finally reader.close()
    }

    override def hasDefiniteSize = true
}

/**
 * XML source which reads from a parsed XML tree.
 */
private class XMLSource(xml : Elem, language: Language) extends Source
{
    override def foreach[U](f : WikiPage => U) : Unit =
    {
        for(page <- xml \ "page";
            rev <- page \ "revision")
        {
            val _contributorID = if ( (rev \ "contributor" \ "id" ).text == null) "0"
                                 else (rev \ "contributor" \ "id" ).text
            f( new WikiPage( title     = WikiTitle.parse((page \ "title").text, language),
                             redirect  = null, // TODO: read redirect title from XML
                             id        = (page \ "id").text,
                             revision  = (rev \ "id").text,
                             timestamp = (rev \ "timestamp").text,
                             contributorID = _contributorID,
                             contributorName = if (_contributorID == "0") (rev \ "contributor" \ "ip" ).text
                                               else (rev \ "contributor" \ "username" ).text,
                             source    = (rev \ "text").text,
                             format    = (rev \ "format").text) )
        }
    }

    override def hasDefiniteSize = true
}

/**
 * OAI XML source which reads from a parsed XML response.
 */
private class OAIXMLSource(xml : Elem) extends Source
{
    override def foreach[U](f : WikiPage => U) : Unit =
    {

        val lang = if ( (xml \\ "mediawiki" \ "@{http://www.w3.org/XML/1998/namespace}lang").text == null) "en"
                   else (xml \\ "mediawiki" \ "@{http://www.w3.org/XML/1998/namespace}lang").text
        val source = new XMLSource( (xml \\ "mediawiki").head.asInstanceOf[Elem], Language.apply(lang))
        source.foreach(wikiPage => {
          f(wikiPage)
        })
    }

    override def hasDefiniteSize = true
}
