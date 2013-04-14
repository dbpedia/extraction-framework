package org.dbpedia.extraction.wikiparser

import java.net.URI

/**
 * Represents a Link.
 * This class is abstract and derived by ExternalLinkNode and InternalLinkNode.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 */
sealed abstract class LinkNode(children : List[Node], line : Int)
extends Node(children, line)
{
    def toPlainText = children.map(_.toPlainText).mkString
}

sealed abstract class WikiLinkNode(destination: WikiTitle, children: List[Node], line: Int, destinationNodes: List[Node]) 
extends LinkNode(children, line)
{
    def toWikiText = "[[" + destination.decodedWithNamespace + "|" + children.map(_.toWikiText).mkString + "]]"
}

/**
 * Represents an external Link.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 * 
 * @param destination The destination URI of this link
 * @param children The nodes of the label of this link
 * @param line The source line number of this link
 */
case class ExternalLinkNode(destination : URI, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]())
extends LinkNode(children, line)
{
    def toWikiText = "[" + destination.toString + " " + children.map(_.toWikiText).mkString + "]"
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
case class InternalLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]())
extends WikiLinkNode(destination, children, line, destinationNodes)

/**
 * Represents an InterWiki Link.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 *
 * @param destination The destination WikiTitle of this link
 * @param children The nodes of the label of this link
 * @param line The source line number of this link
 */
case class InterWikiLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, destinationNodes : List[Node] = List[Node]()) 
extends WikiLinkNode(destination, children, line, destinationNodes)

/**
 * Represents an InterWiki Link in Wikidata.
 * Not really a Wikitext node, so we use dummy values for line and children.
 *
 * @param source The source WikiTitle of this link (wikidata page uri is usually not what one wants to get)
 * @param destination The destination WikiTitle of this link
 */
case class WikidataInterWikiLinkNode(source: WikiTitle, destination : WikiTitle)
extends WikiLinkNode(destination, List[Node](), 0, List[Node]())
