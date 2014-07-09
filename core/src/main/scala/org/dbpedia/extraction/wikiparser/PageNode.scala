package org.dbpedia.extraction.wikiparser
import org.dbpedia.extraction.sources.WikiPage

/**
 * Represents a page.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the latest contributor
 * @param contributorName The name of the latest contributor
 * @param isRedirect True, if this is a Redirect page
 * @param isDisambiguation True, if this is a Disambiguation page
 * @param children The contents of this page
 */
class PageNode (
  val title: WikiTitle, 
  val id: Long, 
  val revision: Long,
  val timestamp: Long,
  val contributorID: Long,
  val contributorName: String,
  val isRedirect: Boolean,
  val isDisambiguation: Boolean,
  children: List[Node] = List.empty
) 
extends Node(children, 0)
{
    def toWikiText = children.map(_.toWikiText).mkString

    def toPlainText = children.map(_.toPlainText).mkString

    def toDumpXML = WikiPage.toDumpXML(title, id, revision, timestamp, contributorID, contributorName, toWikiText, "text/x-wiki")
}
