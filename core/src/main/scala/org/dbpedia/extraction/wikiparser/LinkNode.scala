package org.dbpedia.extraction.wikiparser

import java.net.URI

/**
 * Represents a Link.
 * This class is abstract and derived by ExternalLinkNode and InternalLinkNode.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 */
sealed abstract class LinkNode(override val children : List[Node], override val line : Int) extends Node(children, line)

/**
 * Represents an external Link.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 * 
 * @param destination The destination URI of this link
 * @param children The nodes of the label of this link
 * @param line The source line number of this link
 */
case class ExternalLinkNode(destination : URI, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]()) extends LinkNode(children, line)
{
    def toWikiText() : String = "[" + destination.toString + " " + children.map(_.toWikiText).mkString("") + "]"
}

/**
 * Represents an internal Link.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 * 
 * @param destination The destination WikiTitle of this link
 * @param children The nodes of the label of this link
 * @param line The source line number of this link
 */
case class InternalLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]()) extends LinkNode(children, line)
{
    def toWikiText() : String = "[[" + destination.decodedWithNamespace + "|" + children.map(_.toWikiText).mkString("") + "]]"
}

/**
 * Represents an InterWiki Link.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 *
 * @param destination The destination WikiTitle of this link
 * @param children The nodes of the label of this link
 * @param line The source line number of this link
 */
case class InterWikiLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]()) extends LinkNode(children, line)
{
    def toWikiText() : String = "[[" + destination.decodedWithNamespace + "|" + children.map(_.toWikiText).mkString("") + "]]"
}
