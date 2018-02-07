package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a parser function.
 *
 * @param title The title of the page, where this parser function is defined
 * @param children The properties of this parser function
 * @param line The source line number of this parser function
 */
@WikiNodeAnnotation(classOf[ParserFunctionNode])
case class ParserFunctionNode(title : String, override val children : List[Node], override val line : Int) extends Node
{
    // TODO: check that separating children by pipe is correct
    def toWikiText: String = "{{" + title + ":" + children.map(_.toWikiText).mkString("|") + "}}"
    
    // parser functions are skipped for plain text
    def toPlainText = ""


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
        Option(title),
        if(section != null)
            Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
        else
            None
    )

    override def equals(obj: Any): Boolean = obj match{

        case otherParserFunctionNode : ParserFunctionNode => ( otherParserFunctionNode.title == title //&&  otherParserFunctionNode.line == line
          && NodeUtil.filterEmptyTextNodes(otherParserFunctionNode.children) == NodeUtil.filterEmptyTextNodes(children))
        case _ => false

    }
}
