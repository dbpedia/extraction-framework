package org.dbpedia.extraction.wikiparser

/**
 * Represents a section.
 *
 * @param name The name of this section
 * @param level The level of this section. This corresponds to the number of '=' in the WikiText source
 * @param children The nodes of the section name
 * @param line The source line number of this section
 */
case class SectionNode(name : String, level : Int, override val children : List[Node], override val line : Int) extends Node(children, line)
{
    def toWikiText = ("="*level)+name+("="*level)+"\n"+children.map(_.toWikiText).mkString
    
    def toPlainText = name+"\n\n"+children.map(_.toPlainText).mkString
}