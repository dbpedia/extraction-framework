package org.dbpedia.extraction.sources


import org.dbpedia.extraction.wikiparser.WikiTitle
import java.io.{File,FileInputStream,InputStreamReader}
import xml.Elem
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.mappings.AugmenterExtractorUtils
import org.dbpedia.extraction.util.RichString.toRichString

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 4/6/11
 * Time: 11:28 AM
 * To change this template use File | Settings | File Templates.
 */

object LiveExtractionSource
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

            new WikipediaDumpParser(new InputStreamReader(stream, "UTF-8"), null, null, javaFilter, f).run()

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


              var link = ((page \ "title").text)


              link = link.trim().replaceAll("\\s","_")
              val mytitle = WikiUtil.wikiEncode(link).capitalize(language.locale)
              println(mytitle)

                f( new WikiPage( title     = WikiTitle.parse(link, language),
                                 redirect  = null, // TODO: read redirect title from XML
                                 id        = (page \ "id").text.toLong,
                                 revision  = (rev \ "id").text.toLong,
                                 timestamp = (rev \ "timestamp").text,
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