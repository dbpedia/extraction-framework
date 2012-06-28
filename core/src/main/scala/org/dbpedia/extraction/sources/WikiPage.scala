package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.sources.WikiPage._
import org.dbpedia.extraction.util.StringUtils._

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
class WikiPage(val title: WikiTitle, val redirect: WikiTitle, val id: Long, val revision: Long, val timestamp: Long, val source : String)
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
    def toDumpXML = WikiPage.toDumpXML(title, id, revision, timestamp, source)
}

object WikiPage {
  
  /**
   * XML for one page, Wikipedia dump format.
   * TODO: make sure that XML is valid according to the schema. If not, add dummy elements / attributes where required.
   * TODO: use redirect
   */
  def toDumpXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source : String) = {
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.6/"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.6/ http://www.mediawiki.org/xml/export-0.6.xsd"
      version="0.6"
      xml:lang={title.language.isoCode}>
      <page>
        <title>{title.decodedWithNamespace}</title>
        <ns>{formatInt(title.namespace.code)}</ns>
        <id>{formatLong(id)}</id>
        <revision>
          <id>{formatLong(revision)}</id>
          <timestamp>{formatTimestamp(timestamp)}</timestamp>
          <text xml:space="preserve">{source}</text>
        </revision>
      </page>
    </mediawiki>
  }
  
  /**
   * XML for one page, api.php format.
   * TODO: use redirect
   */
  def toApiXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source : String) = {
    <api>
      <query>
        <pages>
          <page pageid={formatLong(id)} ns={formatInt(title.namespace.code)} title={title.decodedWithNamespace} >
            <revisions>
              <rev revid={formatLong(revision)} timestamp={formatTimestamp(timestamp)} xml:space="preserve">{source}</rev> 
            </revisions>
          </page>
        </pages>
      </query>
    </api>
  }
  
  def parseInt(str: String): Int = {
    if (str == null || str.isEmpty) -1
    else str.toInt
  }
  
  def formatInt(id: Int): String = {
    if (id < 0) ""
    else id.toString
  }
  
  def parseLong(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else str.toLong
  }
  
  def formatLong(id: Long): String = {
    if (id < 0) ""
    else id.toString
  }

}
