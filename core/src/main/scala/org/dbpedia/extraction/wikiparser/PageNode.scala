package org.dbpedia.extraction.wikiparser

/**
 * Represents a page.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param isRedirect True, if this is a Redirect page
 * @param isDisambiguation True, if this is a Disambiguation page
 * @param children The contents of this page
 */
case class PageNode(title : WikiTitle, id : Long, revision : Long, isRedirect : Boolean, isDisambiguation : Boolean,
                    override val children : List[Node] = List.empty) extends Node(children, 0)
{
    def toWikiText() : String = children.map(_.toWikiText).mkString("")
}
