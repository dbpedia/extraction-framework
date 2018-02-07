package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a section.
 *
 * @param name The name of this section
 * @param level The level of this section. This corresponds to the number of '=' in the WikiText source
 * @param children The nodes of the section name
 * @param line The source line number of this section
 */
@WikiNodeAnnotation(classOf[SectionNode])
case class SectionNode(name : String, level : Int, override val children : List[Node], override val line : Int) extends Node
{
    def toWikiText = ("="*level)+name+("="*level)+"\n"+children.map(_.toWikiText).mkString
    
    def toPlainText = name+"\n\n"+children.map(_.toPlainText).mkString

    override def equals(obj: scala.Any) = obj match {

        case otherSectionNode : SectionNode => (otherSectionNode.name == name && otherSectionNode.level == level //&& otherSectionNode.line == line
          && NodeUtil.filterEmptyTextNodes(otherSectionNode.children) == NodeUtil.filterEmptyTextNodes(children))
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
        Option(name),
        if(section != null)
            Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
        else
            None
    )
}