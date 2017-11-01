package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config._
import org.dbpedia.extraction.config.provenance.NodeRecord
import org.dbpedia.extraction.dataparser.RedirectFinder
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Disambiguation

import scala.collection.mutable.ListBuffer
import scala.xml.Elem

/**
 * Represents a page.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the latest contributor
 * @param contributorName The name of the latest contributor
 * @param childNodes The contents of this page
 */
class PageNode (
  val title: WikiTitle,
  val id: Long,
  val revision: Long,
  val timestamp: Long,
  val contributorID: Long,
  val contributorName: String,
  val source: String,
  private var childNodes: List[Node] = List()
) 
extends Node with Recordable[PageNode]
{
  override def children: List[Node] = childNodes

  override val line = 0

  private var extractionRecords: ListBuffer[RecordEntry[PageNode]] = null
  override def recordEntries: List[RecordEntry[PageNode]] = {
    if(extractionRecords == null || extractionRecords.isEmpty)
      List(new WikiPageEntry(this, RecordCause.Internal))
    else
      extractionRecords.toList
  }

  private[extraction] def addExtractionRecord(recordEntry: RecordEntry[_]): Unit ={
    assert(recordEntry != null)
    if(extractionRecords == null)
      extractionRecords = new ListBuffer[RecordEntry[PageNode]]()
    recordEntry match{
      case re: RecordEntry[PageNode] => extractionRecords.append(re)
      case de: RecordEntry[DefaultEntry] => extractionRecords.append(new RecordEntry[PageNode](this, de.cause, Option(de.language).getOrElse(Language.None), de.msg, de.error))
      case _ =>
    }
  }

  private[extraction] def recordError(msg: String): Unit =
    addExtractionRecord(new RecordEntry[PageNode](this, RecordCause.Warning, this.title.language, msg))
  private[extraction] def recordException(ex: Throwable, msg: String = null): Unit =
    addExtractionRecord(new RecordEntry[PageNode](this, RecordCause.Exception, this.title.language, if(msg != null) msg else ex.getMessage, ex))
  private[extraction] def recordMessage(msg: String): Unit =
    addExtractionRecord(new RecordEntry[PageNode](this, RecordCause.Info, this.title.language, msg))
  private[extraction] def recordProvenance = ???

  def toWikiText: String = children.map(_.toWikiText).mkString

  private lazy val _sourcelines = List.empty ++ "x" ++ this.source.lines.toList

  /**
    * returns the original wikitext content of the slice of lines specified
    * @param fromLine - start line of the slice
    * @param toLine - end line of the slice (exclusive), if this parameter is not provided or less than 0, only the fromLine will be returned
    * @return - the concatenated result of all requested lines
    */
  def getOriginWikiText(fromLine: Int, toLine: Int = -1): String = {
    assert(fromLine >= 0)
    val to = if(toLine < 0) fromLine+1 else toLine
    _sourcelines.slice(fromLine, to).mkString("\n")
  }

  def toPlainText: String = children.map(_.toPlainText).mkString

  def toDumpXML: Elem = WikiPage.toDumpXML(title, id, revision, timestamp, contributorID, contributorName, toWikiText, "text/x-wiki")

  lazy val isRedirect: Boolean = this.redirect != null

  lazy val redirect: WikiTitle = {
    val rf = RedirectFinder.getRedirectFinder(title.language)
    rf.apply(this) match{
      case Some(d) => d._2
      case None => null.asInstanceOf[WikiTitle]   //legacy
    }
  }

  def isDisambiguation: Boolean ={
    val disambiguationNames = Disambiguation.get(this.title.language).getOrElse(Set("Disambig"))
    children.exists(node => node.hasTemplate(disambiguationNames))
  }

  //Generate the page URI
  lazy val uri: String = this.title.language.resourceUri.append(this.title.decodedWithNamespace)

  override def equals(other : Any): Boolean = other match
  {

      case otherPageNode : PageNode => ( otherPageNode.title == title && otherPageNode.id == id && otherPageNode.revision == revision && otherPageNode.timestamp == timestamp
        && otherPageNode.contributorID == contributorID && otherPageNode.contributorName == contributorName && otherPageNode.isRedirect == isRedirect
        && otherPageNode.isDisambiguation == isDisambiguation && NodeUtil.filterEmptyTextNodes(otherPageNode.children) == NodeUtil.filterEmptyTextNodes(children))
      case _ => false
  }

  def getNodeRecord = NodeRecord(
    uri = this.uri,
    revision = this.revision,
    namespace = this.title.namespace.code,
    line = this.line,
    language = this.title.language.wikiCode
  )
}