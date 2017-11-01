package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}
import org.dbpedia.extraction.util.Language

/**
 * Represents plain text.
 *
 * @param text The text
 * @param line The source line number where this text begins
 */
case class TextNode(text : String, override val line : Int, lang: Language = null) extends Node
{
    def toWikiText: String = text
    
    def toPlainText: String = text

    def getLanguage: Option[Language] = Option(lang)
    /**
     * Returns the text denoted by this node.
     */
    override protected def retrieveText(recurse: Boolean) = Some(text)

    override def equals(obj: scala.Any): Boolean = obj match {

        case otherTextNode : TextNode => otherTextNode.text.trim == text.trim // && otherTextNode.line == line)
        case _ => false

    }

    override def children = List()

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}
