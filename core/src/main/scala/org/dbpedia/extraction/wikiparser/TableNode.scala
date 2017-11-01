package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}

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

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}

/**
 * Represents a table row
 *
 * @param children The cells of this table row.
 * @param line The (first) line where this table row is located in the source
 */
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

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}

/**
 * Represents a table cell
 *
 * @param children The contents of this cell
 * @param line  The (first) line where this table cell is located in the source
 */
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

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))
}
