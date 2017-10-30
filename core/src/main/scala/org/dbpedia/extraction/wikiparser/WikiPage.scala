package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

import scala.xml.Elem

/**
 * Represents a wiki page
 *
 * TODO: use redirect id to check redirect extractor. Or get rid of redirect extractor.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the latest contributor
 * @param contributorName The name of the latest contributor
 * @param source The WikiText source of this page
 * @param format e.g. "text/x-wiki"
 */
class WikiPage(
    title: WikiTitle,
    id: Long,
    revision: Long,
    timestamp: Long,
    contributorID: Long,
    contributorName: String,
    source : String,
    val format: String)
extends PageNode(title, id, revision, timestamp, contributorID, contributorName,source)
{
  lazy val pageNode: Option[PageNode] = SimpleWikiParser(this)

  //lazy val pageNode = SimpleWikiParser
  def this(title: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source : String, format: String) =
    this(title, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format)

  def this(title: WikiTitle, source : String) =
    this(title, -1, -1, -1, 0, "", source, "")

   def this(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source: String) =
    this(title, id, revision, timestamp, 0, "", source, "")

  override def toString: String = "WikiPage(" + title + "," + id + "," + revision + "," + contributorID + "," + contributorName + "," + source + "," + format + ")"

  override def toDumpXML: Elem = WikiPage.toDumpXML(title, id, revision, timestamp, contributorID, contributorName, source, format)

}

object WikiPage {
    /**
   * XML for one page, Wikipedia dump format.
   * TODO: make sure that XML is valid according to the schema. If not, add dummy elements / attributes where required.
   * TODO: use redirect
   * TODO: use <contributor> / <ip> if contributorID == 0
   */
  def toDumpXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, contributorID: Long, contributorName: String, source: String, format: String): Elem = {
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.8/"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.8/ http://www.mediawiki.org/xml/export-0.8.xsd"
      version="0.8"
      xml:lang={title.language.isoCode}>
      <page>
        <title>{title.decodedWithNamespace}</title>
        <ns>{formatInt(title.namespace.code)}</ns>
        <id>{formatLong(id)}</id>
        <revision>
          <id>{formatLong(revision)}</id>
          <timestamp>{formatTimestamp(timestamp)}</timestamp>
          <contributor>
            <username>{contributorName}</username>
            <id>{formatLong(contributorID)}</id>
          </contributor>
          <text xml:space="preserve">{source}</text>
          <format>{format}</format>
        </revision>
      </page>
    </mediawiki>
  }
  
  /**
   * XML for one page, api.php format.
   * TODO: use redirect
   */
  def toApiXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source: String, format: String): Elem = {
    <api>
      <query>
        <pages>
          <page pageid={formatLong(id)} ns={formatInt(title.namespace.code)} title={title.decodedWithNamespace} >
            <revisions>
              <rev contentformat={format} revid={formatLong(revision)} timestamp={formatTimestamp(timestamp)} xml:space="preserve">{source}</rev> 
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
