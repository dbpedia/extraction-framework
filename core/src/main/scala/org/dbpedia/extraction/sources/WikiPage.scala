package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle
import java.text.{DateFormat,SimpleDateFormat}
import org.dbpedia.extraction.sources.WikiPage._

/**
 * Represents a wiki page
 *
 * TODO: use redirect id to check redirect extractor. Or get rid of redirect extractor.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param source The WikiText source of this page
 */
case class WikiPage(val title: WikiTitle, val redirect: WikiTitle, val id: Long, val revision: Long, val timestamp: Long, val source : String)
{
    def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, source : String) =
      this(title, redirect, parseLong(id), parseLong(revision), parseTimestamp(timestamp), source)
    
    def this(title: WikiTitle, source : String) =
      this(title, null, -1, -1, -1, source)
    
    override def toString = "WikiPage(" + title + "," + id + "," + revision + "," + source + ")"
    
    /**
     * Serializes this page to XML using the MediaWiki export format.
     * The MediaWiki export format is specified at http://www.mediawiki.org/xml/export-0.6.
     */
    def toXML = WikiPage.toDumpXML(title, id, revision, timestamp, source)
}

object WikiPage {
  
  // TODO: this is the XML format used in the dump files, but the format used by api.php is different.
  // TODO: make sure that XML is valid according to the schema. If not, add dummy elements / attributes where required.
  // TODO: use redirect
  def toDumpXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source : String) = {
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.6/"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.6/ http://www.mediawiki.org/xml/export-0.6.xsd"
      version="0.6"
      xml:lang={title.language.isoCode}>
      <page>
        <title>{title.decodedWithNamespace}</title>
        <id>{formatLong(id)}</id>
        <revision>
          <id>{formatLong(revision)}</id>
          <timestamp>{formatTimestamp(timestamp)}</timestamp>
          <text xml:space="preserve">{source}</text>
        </revision>
      </page>
    </mediawiki>
  }
  
  def parseLong(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else str.toLong
  }
  
  def formatLong(id: Long): String = {
    if (id < 0) ""
    else id.toString
  }
  
  // SimpleDateFormat is expensive but not thread-safe
  private val timestampParser = new ThreadLocal[DateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
  }
  
  def parseTimestamp(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else timestampParser.get.parse(str).getTime
  }
  
  def formatTimestamp(timestamp: Long): String = {
    if (timestamp < 0) ""
    else timestampParser.get.format(timestamp)
  }
}
