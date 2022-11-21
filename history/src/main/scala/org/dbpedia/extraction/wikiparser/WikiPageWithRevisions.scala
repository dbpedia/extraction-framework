package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity}
import org.dbpedia.extraction.util.StringUtils._
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
                             title: WikiTitle,
                             redirect: WikiTitle,
                             id: Long,
                             revision: Long,
                             timestamp: Long,
                             contributorID: Long,
                             contributorName: String,
                             source: String,
                             format: String,
                             val revisions: List[RevisionNode]
                           ) extends WikiPage (title,redirect, id, revision,timestamp,contributorID, contributorName,  source,  format)
{
  private val extractionRecords = ListBuffer[RecordEntry[WikiPageWithRevisions]]()

  def this(title: WikiTitle, source: String) =
    this(title, null, -1, -1, -1, 0, "", source, "", List[RevisionNode]())

  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source: String, format: String,revisions: List[RevisionNode]) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format,revisions)

  def this(title: WikiTitle, redirect: WikiTitle, id: String, revision: String, timestamp: String, contributorID: String, contributorName: String, source: String, format: String, revisions: util.ArrayList[RevisionNode]) =
    this(title, redirect, WikiPage.parseLong(id), WikiPage.parseLong(revision), parseTimestamp(timestamp), WikiPage.parseLong(contributorID), contributorName, source, format, WikiPageWithRevisions.convertJavaListRevision(revisions))





  override def toString: String = "WikiPageWithRevision(" + title + "," + id   + ", with " + revisions.size+ "revisions )"

}

object WikiPageWithRevisions  {


  def convertJavaListRevision(myJavaList: util.ArrayList[RevisionNode]):  List[RevisionNode] = {
    myJavaList.asScala.toList
      //.map(x => new RevisionNode(x))
  }


}
