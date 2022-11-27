package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.{RecordEntry2, RecordSeverity}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
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
class WikiPageWithRevisions(
                             val title: WikiTitle,
                             val redirect: WikiTitle,
                             val id: Long,
                             val revision: Long,
                             val timestamp: Long,
                             val contributorID: Long,
                             val contributorName: String,
                             val source: String,
                             val format: String,
                             val revisions: List[RevisionNode]
                           ) extends java.io.Serializable
{
  private val extractionRecords = ListBuffer[RecordEntry2[WikiPageWithRevisions]]()


  lazy val pageNode: Option[PageNode] = SimpleWikiParser(WikiPageWithRevisions.convertToWikiPage(this))

  def uri: String = this.title.language.resourceUri.append(this.title.decodedWithNamespace)

  def isRedirect: Boolean = SimpleWikiParser.getRedirectPattern(title.language).findFirstMatchIn(this.source) match {
    case Some(x) => true
    case None => false
  }

  private var isRetryy = false

  def toggleRetry(): Unit = {
    this.isRetryy = !this.isRetryy
  }

  def isRetry: Boolean = this.isRetryy

  def this(title: WikiTitle, source: String) =
    this(title, null, -1, -1, -1, 0, "", source, "", List[RevisionNode]())

  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source: String, format: String,revisions: List[RevisionNode]) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format,revisions)

  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source: String, format: String) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format, List[RevisionNode]())
  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source: String, format: String, revisions: util.ArrayList[RevisionNode]) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format, WikiPageWithRevisions.convertJavaListRevision(revisions))

  def addExtractionRecord(RecordEntry2: RecordEntry2[WikiPageWithRevisions]): Unit = {
    println("addExtractionRecord")
    val severity = if (RecordEntry2.severity != null)
      RecordEntry2.severity
    else if ((RecordEntry2.errorMsg == null || RecordEntry2.errorMsg.trim.length == 0) && RecordEntry2.error == null)
      RecordSeverity.Info
    else if (RecordEntry2.error != null)
      RecordSeverity.Exception
    else
      RecordSeverity.Warning

    extractionRecords.append(new RecordEntry2(RecordEntry2.page, RecordEntry2.page.uri, severity, RecordEntry2.page.title.language, RecordEntry2.errorMsg, RecordEntry2.error))
  }

  def addExtractionRecord(errorMsg: String = null, error: Throwable = null, severity: RecordSeverity.Value = null): Unit = {

    addExtractionRecord(new RecordEntry2[WikiPageWithRevisions](this, this.uri, severity, this.title.language, errorMsg, error))
  }

  def getExtractionRecords(): mutable.Seq[RecordEntry2[WikiPageWithRevisions]] = this.extractionRecords.seq
  override def toString: String = "WikiPageWithRevision(" + title + "," + id   + ", with " + revisions.size+ "revisions )"
  lazy val sourceIri = title.pageIri + "?" + (if (revision >= 0) "oldid=" + revision + "&" else "") + "ns=" + title.namespace.code

}

object WikiPageWithRevisions  {


  def convertJavaListRevision(myJavaList: util.ArrayList[RevisionNode]):  List[RevisionNode] = {
    myJavaList.asScala.toList
    //.map(x => new RevisionNode(x))
  }

  def convertToWikiPage(wpr: WikiPageWithRevisions): WikiPage = {
    new WikiPage(wpr.title,wpr.redirect,wpr.id,wpr.revision,wpr.timestamp,wpr.source)
  }

}
