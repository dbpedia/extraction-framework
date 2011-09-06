package org.dbpedia.extraction.wikiparser

/**
 * Represents a template property.
 *
 * @param key The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
case class TemplateParameterNode(parameter : String, opt : Boolean, override val line : Int) extends Node(List.empty, line)
{
    def toWikiText() : String = "{{{" + parameter + (if (opt == true) "|" else "") + "}}}"

}