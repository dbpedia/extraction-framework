package org.dbpedia.extraction.wikiparser

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
                      override val line : Int ) extends Node(children, line)
{
    def toWikiText() : String = ""  //TODO implement!!!
}

/**
 * Represents a table row
 *
 * @param children The cells of this table row.
 * @param line The (first) line where this table row is located in the source
 */
case class TableRowNode( override val children : List[TableCellNode],
                         override val line : Int ) extends Node(children, line)
{
    def toWikiText() : String = ""  //TODO implement!!!
}

/**
 * Represents a table cell
 *
 * @param children The contents of this cell
 * @param line  The (first) line where this table cell is located in the source
 */
//TODO include rowspan and columnspan properties
case class TableCellNode( override val children : List[Node],
                          override val line : Int ) extends Node(children, line)
{
    def toWikiText() : String = ""  //TODO implement!!!
}
