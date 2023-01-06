package org.dbpedia.extraction.sources

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiPageWithRevisions, WikiTitle}

import java.io.{File, FileInputStream, InputStreamReader, Reader}
import java.lang
import java.util.concurrent.{Callable, ExecutorService, Executors}
import scala.collection.JavaConversions._
import scala.util.Try
import scala.xml.Elem

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
object XMLSource2
{
  /**
   * Creates an XML Source from an input stream.
   *
   * @param file     The input stream to read from. Will be closed after reading.
   * @param filter   Function to filter pages by their title. Pages for which this function returns false, won't be yielded by the source.
   * @param language if given, parser expects file to be in this language and doesn't read language from siteinfo element
   */
  def fromFile(file: File, language: Language, filter: WikiTitle => Boolean = _ => true): Source2 = {
    fromReader(() => new InputStreamReader(new FileInputStream(file), "UTF-8"), language, filter)
  }

  def fromMultipleFiles(files: List[File], language: Language, filter: WikiTitle => Boolean = _ => true): Source2 = {
    fromReaders(files.map { f => () => new InputStreamReader(new FileInputStream(f), "UTF-8") }, language, filter)
  }

    /**
     * Creates an XML Source from a reader.
     *
     * @param source The input stream to read from. Will be closed after reading.
     * @param filter Function to filter pages by their title. Pages for which this function returns false, won't be yielded by the source.
     * @param language if given, parser expects file to be in this language and doesn't read language from siteinfo element
     */
    def fromReader(source: () => Reader, language: Language, filter: WikiTitle => Boolean = _ => true) : Source2 = {

      System.out.print("fromReader - XMLReaderSource2 ");
      new XMLReaderSource2(source, language, filter)
    }

  def fromReaders(sources: Seq[() => Reader], language: Language, filter: WikiTitle => Boolean = _ => true): Source2 = {

    System.out.print("fromReaders - MultipleXMLReaderSource2 ");
    if (sources.size == 1) fromReader(sources.head, language, filter) // no need to create an ExecutorService
    else new MultipleXMLReaderSource2(sources, language, filter)
  }

    /**
     *  Creates an XML Source from a parsed XML tree.
     *
     * @param xml The xml which contains the pages
     */
  //  def fromXML(xml : Elem, language: Language) : Source2  = new XMLSource2(xml, language)

    /**
       *  Creates an XML Source from a parsed XML OAI response.
       *
       * @param xml The xml which contains the pages
     */
   // def fromOAIXML(xml : Elem) : Source2  = new OAIXMLSource2(xml)

}

/**
 * XML source which reads from a file
 */
private class MultipleXMLReaderSource2(sources: Seq[() => Reader], language: Language, filter: WikiTitle => Boolean) extends Source2
{
  var executorService : ExecutorService = _

  override def foreach[U](proc : WikiPageWithRevisions => U) : Unit = {

    System.out.print("MultipleXMLReaderSource2 ");
    if (executorService == null) executorService = Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())

    try {

      def tasks = sources.map { source =>
        new Callable[Unit]() {
          def call() {
            val reader = source()

            try new WikipediaDumpParserHistory(reader, language, filter.asInstanceOf[WikiTitle => lang.Boolean], proc).run()
            finally reader.close()
          }
        }
      }

      // Wait for the tasks to finish
      executorService.invokeAll(tasks)

    } finally {
      executorService.shutdown()
      executorService = null
    }
  }

  override def hasDefiniteSize = true
}

/**
 * XML source which reads from a file
 */
private class XMLReaderSource2(source: () => Reader, language: Language, filter: WikiTitle => Boolean) extends Source2
{
    override def foreach[U](proc : WikiPageWithRevisions => U) : Unit = {
      System.out.print("XMLReaderSource2 - readPages")
      val reader = source()
      try new WikipediaDumpParserHistory(reader, language, filter.asInstanceOf[WikiTitle => java.lang.Boolean], proc).run()
      finally reader.close()
      System.out.print("END XMLReaderSource2 - readPages")

    }

    override def hasDefiniteSize = true
}

/**
 * XML source which reads from a parsed XML tree.
 */
/*private class XMLSource2(xml : Elem, language: Language) extends Source2
{
    override def foreach[U](f : WikiPageWithRevisions => U) : Unit =
    {

      for(page <- xml \ "page";
          rev <- page \ "revision")
      {

        val title = WikiTitle.parseCleanTitle((page \ "title").text, language, Try{new java.lang.Long(java.lang.Long.parseLong((page \ "id").text))}.toOption)

        val nsElem = page \ "ns"
        if (nsElem.nonEmpty )
        {
          try
          {
            val nsCode = nsElem.text.toInt
            require(title.namespace.code == nsCode, "XML Namespace (" + nsCode + ") does not match the computed namespace (" + title.namespace + ") in page: " + title.decodedWithNamespace)
          }
          catch
          {
            case e: NumberFormatException => throw new IllegalArgumentException("Cannot parse content of element [ns] as int", e)
          }
        }

        //Skip bad titles
        if(title != null)
        {
            val _redirect = (page \ "redirect" \ "@title").text match
            {
              case "" => null
              case t => WikiTitle.parse(t, language)
            }
            val _contributorID = (rev \ "contributor" \ "id").text match
            {
              case null => "0"
              case id => id
            }
           /* f( new WikiPageWithRevisions( title     = title,
                             redirect  = _redirect,
                             id        = (page \ "id").text,
                             revision  = (rev \ "id").text,
                             timestamp = (rev \ "timestamp").text,
                             contributorID = _contributorID,
                             contributorName = if (_contributorID == "0") (rev \ "contributor" \ "ip" ).text
                             else (rev \ "contributor" \ "username" ).text,
                             source    = (rev \ "text").text,
                             format    = (rev \ "format").text) */
        }
      }
    }

    override def hasDefiniteSize = true
}*/

/**
 * OAI XML source which reads from a parsed XML response.
 */
/*private class OAIXMLSource2(xml : Elem) extends Source2
{
    override def foreach[U](f : WikiPage => U) : Unit =
    {

        val lang = if ( (xml \\ "mediawiki" \ "@{http://www.w3.org/XML/1998/namespace}lang").text == null) "en"
                   else (xml \\ "mediawiki" \ "@{http://www.w3.org/XML/1998/namespace}lang").text
        val source = new XMLSource2( (xml \\ "mediawiki").head.asInstanceOf[Elem], Language.apply(lang))
        source.foreach(wikiPageWithRevisions => {
          f(wikiPageWithRevisions)
        })
    }

    override def hasDefiniteSize = true
}*/
