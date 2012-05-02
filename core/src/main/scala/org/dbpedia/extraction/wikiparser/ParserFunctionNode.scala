package org.dbpedia.extraction.wikiparser

/**
 * Represents a parser function.
 *
 * @param title The title of the page, where this parser function is defined
 * @param children The properties of this parser function
 * @param line The source line number of this parser function
 */
case class ParserFunctionNode(title : String, override val children : List[Node], override val line : Int) extends Node(children, line)
{
    def toWikiText() : String = "{{" + title + ":" + children.map(_.toWikiText()) + "}}"
}
