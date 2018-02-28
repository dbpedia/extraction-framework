package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.NodeRecord
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

@WikiNodeAnnotation(classOf[ParserFunctionParameterNode])
case class ParserFunctionParameterNode(override val children : List[Node], override val line : Int) extends Node{
  /**
    * Convert back to original (or equivalent) wiki markup string.
    */
  override def toWikiText = if (children.isEmpty) "" else "|"+children.map(_.toWikiText).mkString

  /**
    * Get plain text content of this node and all child nodes, without markup. Since templates
    * are not expanded, this will not work well for templates.
    */
  override def toPlainText = ""

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
