package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}

/**
 * Represents a parser function.
 *
 * @param title The title of the page, where this parser function is defined
 * @param children The properties of this parser function
 * @param line The source line number of this parser function
 */
case class ParserFunctionNode(title : String, override val children : List[Node], override val line : Int) extends Node
{
    // TODO: check that separating children by pipe is correct
    def toWikiText: String = "{{" + title + ":" + children.map(_.toWikiText).mkString("|") + "}}"
    
    // parser functions are skipped for plain text
    def toPlainText = ""

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))

    override def equals(obj: Any): Boolean = obj match{

        case otherParserFunctionNode : ParserFunctionNode => ( otherParserFunctionNode.title == title //&&  otherParserFunctionNode.line == line
          && NodeUtil.filterEmptyTextNodes(otherParserFunctionNode.children) == NodeUtil.filterEmptyTextNodes(children))
        case _ => false

    }
}
