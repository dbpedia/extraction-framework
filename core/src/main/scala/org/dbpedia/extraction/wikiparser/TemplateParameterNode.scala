package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}

/**
 * Represents a template property.
 *
 * @param parameter The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
case class TemplateParameterNode(parameter : String, override val children : List[Node], override val line : Int) extends Node
{
    def toWikiText = {
      val rest = if (children.isEmpty) "" else "|"+children.map(_.toWikiText).mkString
      "{{{"+parameter+rest+"}}}"
    }
    
    // template parameters are skipped for plain text
    def toPlainText = ""

  override def equals(obj: scala.Any) = obj match {

    case otherTPN : TemplateParameterNode => (otherTPN.parameter == parameter  //&& otherTPN.line == line
      && NodeUtil.filterEmptyTextNodes(otherTPN.children) == NodeUtil.filterEmptyTextNodes(children))
    case _ => false

  }

  override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}