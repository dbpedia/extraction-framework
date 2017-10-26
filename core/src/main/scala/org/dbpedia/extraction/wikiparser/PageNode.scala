package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.dataparser.RedirectFinder
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Disambiguation

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
 * @param children The contents of this page
 */
class PageNode (
  val title: WikiTitle, 
  val id: Long, 
  val revision: Long,
  val timestamp: Long,
  val contributorID: Long,
  val contributorName: String,
  val source: String,
  children: List[Node] = List.empty
) 
extends Node(children, 0) with WikiTitleHolder
{
  def toWikiText: String = children.map(_.toWikiText).mkString

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
    children.exists(node => findTemplate(node, disambiguationNames))
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


  private def findTemplate(node : Node, names : Set[String]) : Boolean = node match
  {
    case TemplateNode(title, _, _, _) => names.contains(title.decoded)
    case _ => node.children.exists(node => findTemplate(node, names))
  }
}