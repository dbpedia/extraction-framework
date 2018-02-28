package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a Link.
 * This class is abstract and derived by ExternalLinkNode and InternalLinkNode.
 * The children of this node represent the label of the link.
 * If the source does not define a label explicitly, a TextNode containing the link destination will be the only child.
 */
@WikiNodeAnnotation(classOf[LinkNode])
sealed abstract class LinkNode(children : List[Node], override val line : Int, val destinationNodes : List[Node])
extends Node
{
    def toPlainText = children.map(_.toPlainText).mkString


    /**
      * Creates a NodeRecord metadata object of this node
      *
      * @return
      */
    override def getNodeRecord = NodeRecord(
        IRI.create(this.sourceIri).get,
        this.wikiNodeAnnotation,
        this.root.revision,
        this.root.title.namespace.code,
        this.id,
        this.root.title.language,
        Option(this.line),
        None,
        if(section != null)
            Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
        else
            None
    )
}

@WikiNodeAnnotation(classOf[WikiLinkNode])
sealed abstract class WikiLinkNode(destination: WikiTitle, children: List[Node], line: Int, destinationNodes: List[Node]) 
extends LinkNode(children, line, destinationNodes)
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
@WikiNodeAnnotation(classOf[ExternalLinkNode])
case class ExternalLinkNode(destination : IRI, override val children : List[Node], override val line : Int, override val destinationNodes : List[Node] = List[Node]())
extends LinkNode(children, line, destinationNodes)
{
    def toWikiText: String = "[" + destination.toString + " " + children.map(_.toWikiText).mkString + "]"
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
@WikiNodeAnnotation(classOf[InternalLinkNode])
case class InternalLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, override val destinationNodes : List[Node] = List[Node]())
extends WikiLinkNode(destination, children, line, destinationNodes) {
    override def equals(obj: scala.Any) = obj match {
        case otherLink : InternalLinkNode => otherLink.destination == destination && NodeUtil.filterEmptyTextNodes(otherLink.children) == NodeUtil.filterEmptyTextNodes(children)
        case _ => false
    }
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
@WikiNodeAnnotation(classOf[InterWikiLinkNode])
case class InterWikiLinkNode(destination : WikiTitle, override val children : List[Node], override val line : Int, override val destinationNodes : List[Node] = List[Node]())
extends WikiLinkNode(destination, children, line, destinationNodes)
