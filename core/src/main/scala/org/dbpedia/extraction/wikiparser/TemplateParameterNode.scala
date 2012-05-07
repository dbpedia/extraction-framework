package org.dbpedia.extraction.wikiparser

/**
 * Represents a template property.
 *
 * @param key The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
case class TemplateParameterNode(parameter : String, override val children : List[Node], override val line : Int) extends Node(children, line)
{
    def toWikiText(): String = {
      val rest = if (children.isEmpty) "" else "|"+children.map(_.toWikiText).mkString("")
      "{{{"+parameter+rest+"}}}"
    }
}