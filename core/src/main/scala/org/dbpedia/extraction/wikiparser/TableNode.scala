package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a table.
 * 
 * The rows are represents as child nodes.
 * Each row itself contains a child node for each of its cells.
 *
 * @param caption The caption of this table
 * @param children The rows of this table
 * @param line The (first) line where this table is located in the source
 */
@WikiNodeAnnotation(classOf[TableNode])
case class TableNode( caption : Option[String],
                      override val children : List[TableRowNode],
                      override val line : Int )
  extends Node
{
    def toWikiText = ""  //TODO implement!!!
    def toPlainText = ""  //TODO implement!!!
    override def equals(obj: scala.Any) = obj match
            {
            case otherTableNode : TableNode => (otherTableNode.caption.getOrElse("") == caption.getOrElse("")   //&& otherTableNode.line == line
              && NodeUtil.filterEmptyTextNodes(otherTableNode.children) == NodeUtil.filterEmptyTextNodes( children))
            case _ => false
        }

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(absoluteLine = Some(this.line), name = caption)
}

/**
 * Represents a table row
 *
 * @param children The cells of this table row.
 * @param line The (first) line where this table row is located in the source
 */
@WikiNodeAnnotation(classOf[TableRowNode])
case class TableRowNode( override val children : List[TableCellNode],
                         override val line : Int ) extends Node
{
    def toWikiText = ""  //TODO implement!!!
    def toPlainText = ""  //TODO implement!!!
    override def equals(obj: scala.Any) = obj match
        {
            case otherTableRowNode : TableRowNode => NodeUtil.filterEmptyTextNodes(otherTableRowNode.children) == NodeUtil.filterEmptyTextNodes(children)
            case _ => false
        }

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(absoluteLine = Some(this.line))
}

/**
 * Represents a table cell
 *
 * @param children The contents of this cell
 * @param line  The (first) line where this table cell is located in the source
 */
@WikiNodeAnnotation(classOf[TableCellNode])
case class TableCellNode (
  override val children : List[Node],
  override val line: Int,
  var rowSpan: Int, // FIXME: var for TableMapping.preprocessTable()
  var colSpan: Int // FIXME: var for TableMapping.preprocessTable()

) 
extends Node
{
    def toWikiText = ""  //TODO implement!!!
    def toPlainText = ""  //TODO implement!!!
    override def equals(obj: scala.Any) = obj match
        {
        case otherTableCellNode : TableCellNode => (otherTableCellNode.rowSpan == rowSpan
          && otherTableCellNode.colSpan == colSpan && NodeUtil.filterEmptyTextNodes(otherTableCellNode.children) == NodeUtil.filterEmptyTextNodes(children))
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
        None,
        if(section != null)
            Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
        else
            None
    )
}
