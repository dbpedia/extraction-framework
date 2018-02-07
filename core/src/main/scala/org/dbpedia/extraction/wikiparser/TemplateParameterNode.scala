package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a template property.
 *
 * @param parameter The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
@WikiNodeAnnotation(classOf[TemplateParameterNode])
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
    Option(parameter),
    if(section != null)
      Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
    else
      None
  )
}