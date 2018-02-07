package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.NodeRecord
import org.dbpedia.extraction.util.{Language, WikiUtil}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.iri.IRI

/**
 * Represents plain text.
 *
 * @param text The text
 * @param line The source line number where this text begins
 */

@WikiNodeAnnotation(classOf[TextNode])
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
