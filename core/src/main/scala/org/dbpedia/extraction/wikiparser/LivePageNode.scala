package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.StringUtils._

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 9/14/11
 * Time: 4:09 PM
 *
 * Represents a page for live extraction, as live extraction needs more meta-data e.g. timestamp of last change.
 *
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param isRedirect True, if this is a Redirect page
 * @param isDisambiguation True, if this is a Disambiguation page
 * @param children The contents of this page
 */

case class LivePageNode(override val title : WikiTitle, override val id : Long, override val revision : Long,
                        override val isRedirect : Boolean, override val isDisambiguation : Boolean,
                        val nodeTimestamp: String, val contributorID: Long, val contributorName: String, override val children : List[Node] = List.empty)
  extends PageNode(title, id, revision, parseTimestamp(nodeTimestamp), isRedirect, isDisambiguation, children)
{
  override def toWikiText() : String = children.map(_.toWikiText).mkString("")

  // TODO: see WikiPage.toDumpXML(): use formatInt, formatLong, formatTimestamp
  override def toDumpXML =
  {
    <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.4/"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.4/ http://www.mediawiki.org/xml/export-0.4.xsd"
               version="0.4"
               xml:lang="en">
      <page>
        <title>{title.decodedWithNamespace}</title>
        <id>{id}</id>
        <revision>
          <contributor>
            <username>{contributorName}</username>
            <id>{contributorID}</id>
          </contributor>
          <id>{revision}</id>
          <timestamp>{timestamp}</timestamp>
          <text xml:space="preserve">{toWikiText()}</text>
        </revision>
      </page>
    </mediawiki>
  }
}