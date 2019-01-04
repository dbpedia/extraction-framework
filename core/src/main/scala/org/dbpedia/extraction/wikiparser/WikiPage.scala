package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
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
    val title: WikiTitle,
    val redirect: WikiTitle,
    val id: Long,
    val revision: Long,
    val timestamp: Long,
    val contributorID: Long,
    val contributorName: String,
    val source : String,
    val format: String) extends java.io.Serializable
{

  private val extractionRecords = ListBuffer[RecordEntry[WikiPage]]()

  lazy val pageNode: Option[PageNode] = SimpleWikiParser(this)

  def isRedirect: Boolean = SimpleWikiParser.getRedirectPattern(title.language).findFirstMatchIn(this.source) match{
    case Some(x) => true
    case None => false
  }

  def isDisambiguation: Boolean = pageNode match{
    case Some(s) => s.isDisambiguation
    case None => throw new WikiParserException("WikiPage " + title.encoded + " could not be extracted.")
  }
  //lazy val pageNode = SimpleWikiParser
  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source : String, format: String) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format)

  def this(title: WikiTitle, source : String) =
    this(title, null, -1, -1, -1, 0, "", source, "")

   def this(title: WikiTitle, redirect: WikiTitle, id: Long, revision: Long, timestamp: Long, source: String) =
    this(title, redirect, id, revision, timestamp, 0, "", source, "")

  override def toString: String = "WikiPage(" + title + "," + id + "," + revision + "," + contributorID + "," + contributorName + "," + source + "," + format + ")"

  private var _sourceIri: String = _
  def sourceIri : String = {
    if(_sourceIri == null)
      _sourceIri = title.pageIri + "?" + (if (revision >= 0) "oldid=" + revision + "&" else "") + "ns=" + title.namespace.code
    _sourceIri
  }

  //Generate the page URI
  def uri: String = this.title.language.resourceUri.append(this.title.decodedWithNamespace)

  def toDumpXML: Elem = WikiPage.toDumpXML(title, id, revision, timestamp, contributorID, contributorName, source, format)

  private var isRetryy = false

  def toggleRetry(): Unit = {
    this.isRetryy = !this.isRetryy
  }

  def isRetry: Boolean = this.isRetryy


  def addExtractionRecord(recordEntry: RecordEntry[WikiPage]): Unit ={
    val severity = if(recordEntry.severity != null)
      recordEntry.severity
    else if((recordEntry.errorMsg == null || recordEntry.errorMsg.trim.length == 0) && recordEntry.error == null)
      RecordSeverity.Info
    else if(recordEntry.error != null)
      RecordSeverity.Exception
    else
      RecordSeverity.Warning

    extractionRecords.append(new RecordEntry(recordEntry.page, recordEntry.page.uri, severity, recordEntry.page.title.language, recordEntry.errorMsg, recordEntry.error))
  }

  def addExtractionRecord(errorMsg: String = null, error: Throwable = null, severity: RecordSeverity.Value = null): Unit ={

    addExtractionRecord(new RecordEntry[WikiPage](this, this.uri, severity, this.title.language, errorMsg, error))
  }

  def getExtractionRecords(): mutable.Seq[RecordEntry[WikiPage]] = this.extractionRecords.seq
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
  def toApiXML(title: WikiTitle, id: Long, revision: Long, timestamp: Long, source: String, format: String) = {
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
