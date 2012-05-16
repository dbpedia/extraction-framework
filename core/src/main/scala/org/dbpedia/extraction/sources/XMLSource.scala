package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle
import java.io.{File,FileInputStream,InputStreamReader}
import scala.xml.Elem
import org.dbpedia.extraction.util.Language

/**
 *  Loads wiki pages from an XML stream using the MediaWiki export format.
 * 
 *  The MediaWiki export format is specified by 
 *  http://www.mediawiki.org/xml/export-0.4
 *  http://www.mediawiki.org/xml/export-0.5
 *  http://www.mediawiki.org/xml/export-0.6
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
     * @param namespaceUri expected mediawiki dump file namespace URI, e.g. "http://www.mediawiki.org/xml/export-0.6/"
     */
    def fromFile(file : File, language : Language, filter : (WikiTitle => Boolean) = (title => true), namespaceUri : String = null) : Source = {
      new XMLFileSource(file, language, filter, namespaceUri)
    }

    /**
     *  Creates an XML Source from a parsed XML tree.
     *
     * @param xml The xml which contains the pages
     */
    def fromXML(xml : Elem, language: Language) : Source  = new XMLSource(xml, language)

    /**
     * XML source which reads from a file
     */
    private class XMLFileSource(file: File, language: Language, filter: (WikiTitle => Boolean), namespaceUri: String = null) extends Source
    {
        override def foreach[U](proc : WikiPage => U) : Unit = {
            val jfilter = { title : WikiTitle => filter(title) : java.lang.Boolean }
            val stream = new FileInputStream(file)
            try {
              val reader = new InputStreamReader(stream, "UTF-8")
              new WikipediaDumpParser(reader, namespaceUri, language, jfilter, proc).run()
            }
            finally stream.close()
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
                f( new WikiPage( title     = WikiTitle.parse((page \ "title").text, language),
                                 redirect  = null, // TODO: read redirect title from XML 
                                 id        = (page \ "id").text,
                                 revision  = (rev \ "id").text,
                                 timestamp = (rev \ "timestamp").text,
                                 source    = (rev \ "text").text ) )
            }
        }

        override def hasDefiniteSize = true
    }
}
