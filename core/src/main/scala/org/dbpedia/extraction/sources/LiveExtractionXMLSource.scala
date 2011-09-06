package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle
import java.io.{FileInputStream, File}
import xml.Elem
import org.dbpedia.extraction.util.{WikiUtil, Language}

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 9, 2010
 * Time: 11:14:59 AM
 * This object is responsible for reading the source of the wikipage from a file or from and XML element 
 */

object LiveExtractionXMLSource
{
    /**
     * Creates an XML Source from an input stream.
     *
     * @param stream The input stream to read from. Will be closed after reading.
     * @param filter Function to filter pages by their title. Pages for which this function returns false, won't be yielded by the source.
     */
    def fromFile(file : File, filter : (WikiTitle => Boolean) = (title => true)) : Source = new MyXMLFileSource(file, filter)

    /**
     *  Creates an XML Source from a parsed XML tree.
     *
     * @param xml The xml which contains the pages
     */
    def fromXML(xml : Elem) : Source  = new XMLSource(xml)

    /**
     * XML source which reads from a file
     */
    private class MyXMLFileSource(file : File, filter : (WikiTitle => Boolean)) extends Source
    {
        override def foreach[U](f : WikiPage => U) : Unit =
        {
            val javaFilter = { title : WikiTitle => filter(title) : java.lang.Boolean }
            val stream = new FileInputStream(file)

            new WikipediaDumpParser(stream, f, javaFilter).run()

            stream.close()
        }

        override def hasDefiniteSize = true
    }

    /**
     * XML source which reads from a parsed XML tree.
     */
    private class XMLSource(xml : Elem) extends Source
    {
      //println("xml element = "+ xml + "End of xml element");
        override def foreach[U](f : WikiPage => U) : Unit =
        {

          //val MyPage =  xml \\ "page";
          //println("LiveExtractionXMLSource PAGE = "+ MyPage);
            //TODO set correct language
            val language = Language.Default

            for(page <- xml \\ "page";
                rev <- page \\ "revision")
            {
              //println((page \ "title").text);
              //println((page \ "id").text);


              val link = ((page \ "title").text)

//              org.dbpedia.extraction.mappings.
//              AugmenterExtractorUtils.canonicalize("")
              /*val mytitle = WikiUtil.wikiEncode(link, language)
              println(mytitle)*/

                f( new WikiPage( title     = WikiTitle.parse(link, language),
                                 id        = (page \ "id").text.toLong,
                                 revision  = (rev \ "id").text.toLong,
                                 source    = (rev \ "text").text ) )

              /*f( new WikiPage( title     = mytitle,
                                 id        = (page \ "id").text.toLong,
                                 revision  = (rev \ "id").text.toLong,
                                 source    = (rev \ "text").text ) )*/


            }
        }

        override def hasDefiniteSize = true
    }
}
