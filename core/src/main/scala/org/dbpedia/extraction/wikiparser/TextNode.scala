package org.dbpedia.extraction.wikiparser

/**
 * Represents plain text.
 *
 * @param text The text
 * @param line The source line number where this text begins
 */
case class TextNode(text : String, override val line : Int) extends Node(List.empty, line)
{
    def toWikiText() : String = text
}
