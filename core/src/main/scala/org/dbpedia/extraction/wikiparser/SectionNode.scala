package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}

/**
 * Represents a section.
 *
 * @param name The name of this section
 * @param level The level of this section. This corresponds to the number of '=' in the WikiText source
 * @param children The nodes of the section name
 * @param line The source line number of this section
 */
case class SectionNode(name : String, level : Int, override val children : List[Node], override val line : Int) extends Node
{
    def toWikiText = ("="*level)+name+("="*level)+"\n"+children.map(_.toWikiText).mkString
    
    def toPlainText = name+"\n\n"+children.map(_.toPlainText).mkString

    override def equals(obj: scala.Any) = obj match {

        case otherSectionNode : SectionNode => (otherSectionNode.name == name && otherSectionNode.level == level //&& otherSectionNode.line == line
          && NodeUtil.filterEmptyTextNodes(otherSectionNode.children) == NodeUtil.filterEmptyTextNodes(children))
        case _ => false

    }

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}